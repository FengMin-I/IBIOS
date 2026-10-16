package com.ibios.core.react;

import com.ibios.core.profile.Profile;
import com.ibios.core.profile.ProfileRegistry;
import com.ibios.core.schedule.ScheduledTaskStore;
import com.ibios.core.schedule.ScheduledTaskView;
import com.ibios.core.schedule.TaskExecutionView;
import com.ibios.core.session.Session;
import com.ibios.core.session.SessionManager;
import jakarta.annotation.PostConstruct;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

@Component
public class AgentScheduler {

  private static final Logger LOG = Logger.getLogger(AgentScheduler.class.getName());

  private final TaskScheduler taskScheduler;
  private final ProfileRegistry profileRegistry;
  private final AgentService agentService;
  private final SessionManager sessionManager;
  private final ScheduledTaskStore taskStore;
  private final Map<String, Lock> taskLocks = new ConcurrentHashMap<>();
  private final Map<String, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

  public AgentScheduler(
      TaskScheduler taskScheduler,
      ProfileRegistry profileRegistry,
      AgentService agentService,
      SessionManager sessionManager,
      ScheduledTaskStore taskStore) {
    this.taskScheduler = taskScheduler;
    this.profileRegistry = profileRegistry;
    this.agentService = agentService;
    this.sessionManager = sessionManager;
    this.taskStore = taskStore;
  }

  @PostConstruct
  public void registerAll() {
    for (Profile profile : profileRegistry.getAll()) {
      registerProfile(profile);
    }
  }

  public void registerProfile(Profile profile) {
    for (Profile.ScheduleConfig sc : profile.schedules()) {
      ZoneId zoneId = ZoneId.of(sc.zone());
      Instant nextRun = computeNextRun(sc.cron(), zoneId);
      ScheduledTaskView view =
          new ScheduledTaskView(
              sc.id(),
              profile.name(),
              sc.cron(),
              sc.zone(),
              sc.message(),
              true,
              nextRun,
              null,
              null,
              0,
              Instant.now());
      taskStore.register(view);

      ScheduledFuture<?> future =
          taskScheduler.schedule(() -> runOnce(profile, sc), new CronTrigger(sc.cron(), zoneId));
      scheduledTasks.put(sc.id(), future);
      LOG.info(
          "Registered scheduled task '"
              + sc.id()
              + "' for profile '"
              + profile.name()
              + "', next run: "
              + nextRun);
    }
  }

  public Map<String, ScheduledFuture<?>> scheduledTasks() {
    return Collections.unmodifiableMap(scheduledTasks);
  }

  void runOnce(Profile profile, Profile.ScheduleConfig sc) {
    ScheduledTaskView current = taskStore.get(sc.id());
    if (current != null && !current.enabled()) {
      LOG.info("Task '" + sc.id() + "' is disabled, skip this trigger");
      return;
    }

    Lock lock = taskLocks.computeIfAbsent(sc.id(), id -> new ReentrantLock());
    if (!lock.tryLock()) {
      LOG.info("Task '" + sc.id() + "' still running, skip this trigger");
      return;
    }

    Instant startedAt = Instant.now();
    String sessionId = null;
    boolean success = false;
    String errorMessage = null;
    try {
      Session session = sessionManager.getOrCreate("scheduler", "scheduler", profile.name());
      sessionId = session.id();
      agentService.process(session, sc.message());
      success = true;
    } catch (Exception e) {
      errorMessage = e.getMessage();
      LOG.log(Level.SEVERE, "Scheduled task '" + sc.id() + "' failed", e);
    } finally {
      lock.unlock();
    }

    long durationMs = java.time.Duration.between(startedAt, Instant.now()).toMillis();
    int newRunCount = (current != null ? current.runCount() : 0) + 1;
    ZoneId zoneId = ZoneId.of(sc.zone());
    Instant nextRun = computeNextRun(sc.cron(), zoneId);

    taskStore.updateStatus(
        sc.id(), startedAt, success ? "success" : "failed", nextRun, newRunCount);
    taskStore.recordExecution(
        new TaskExecutionView(sc.id(), sessionId, startedAt, success, errorMessage, durationMs));
  }

  public void runNow(String taskId) {
    for (Profile profile : profileRegistry.getAll()) {
      for (Profile.ScheduleConfig sc : profile.schedules()) {
        if (sc.id().equals(taskId)) {
          runOnce(profile, sc);
          return;
        }
      }
    }
    throw new IllegalArgumentException("Scheduled task not found: " + taskId);
  }

  public ScheduledTaskStore taskStore() {
    return taskStore;
  }

  Lock lockFor(String taskId) {
    return taskLocks.computeIfAbsent(taskId, id -> new ReentrantLock());
  }

  private Instant computeNextRun(String cron, ZoneId zoneId) {
    try {
      CronTrigger trigger = new CronTrigger(cron, zoneId);
      Instant now = Instant.now();
      org.springframework.scheduling.support.SimpleTriggerContext ctx =
          new org.springframework.scheduling.support.SimpleTriggerContext(now, now, now);
      return trigger.nextExecution(ctx);
    } catch (Exception e) {
      LOG.log(Level.WARNING, "Failed to compute next run for cron: " + cron, e);
    }
    return null;
  }
}
