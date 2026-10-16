package com.ibios.core.react;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.ibios.core.profile.Profile;
import com.ibios.core.schedule.ScheduledTaskStore;
import com.ibios.core.session.SessionManager;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.TaskScheduler;

class AgentSchedulerRegisterTest {

  private TaskScheduler taskScheduler;
  private com.ibios.core.profile.ProfileRegistry profileRegistry;
  private AgentService agentService;
  private SessionManager sessionManager;
  private ScheduledTaskStore taskStore;
  private AgentScheduler scheduler;

  @BeforeEach
  void setUp() {
    taskScheduler = mock(TaskScheduler.class);
    profileRegistry = new com.ibios.core.profile.ProfileRegistry();
    agentService = mock(AgentService.class);
    sessionManager = mock(SessionManager.class);
    taskStore = mock(ScheduledTaskStore.class);
    scheduler =
        new AgentScheduler(taskScheduler, profileRegistry, agentService, sessionManager, taskStore);
  }

  @Test
  @DisplayName("registerProfile 后 scheduledTasks 有句柄")
  void registerProfileCreatesHandle() {
    Profile.ScheduleConfig sc =
        new Profile.ScheduleConfig("test-cron", "0 0 9 * * *", "Asia/Shanghai", "hello");
    Profile profile = createProfileWithSchedule(sc);

    when(taskScheduler.schedule(
            any(Runnable.class), any(org.springframework.scheduling.Trigger.class)))
        .thenReturn(mock(java.util.concurrent.ScheduledFuture.class));

    scheduler.registerProfile(profile);

    Map<String, java.util.concurrent.ScheduledFuture<?>> tasks = scheduler.scheduledTasks();
    assertTrue(tasks.containsKey("test-cron"));
  }

  @Test
  @DisplayName("cron/时区来自 Profile.schedules")
  void cronAndZoneFromProfile() {
    Profile.ScheduleConfig sc =
        new Profile.ScheduleConfig("zone-cron", "0 30 8 * * MON", "US/Eastern", "monday task");
    Profile profile = createProfileWithSchedule(sc);

    when(taskScheduler.schedule(
            any(Runnable.class), any(org.springframework.scheduling.Trigger.class)))
        .thenReturn(mock(java.util.concurrent.ScheduledFuture.class));

    scheduler.registerProfile(profile);

    verify(taskScheduler)
        .schedule(any(Runnable.class), any(org.springframework.scheduling.Trigger.class));
  }

  private Profile createProfileWithSchedule(Profile.ScheduleConfig sc) {
    return new Profile(
        "test-profile",
        "test",
        new Profile.Identity("agent", "prompt"),
        new Profile.ProviderConfig("dashscope", "qwen-plus", null),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(sc),
        List.of(),
        Map.of(),
        10,
        20,
        null);
  }
}
