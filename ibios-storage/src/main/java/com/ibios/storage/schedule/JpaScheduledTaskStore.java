package com.ibios.storage.schedule;

import com.ibios.core.schedule.ScheduledTaskStore;
import com.ibios.core.schedule.ScheduledTaskView;
import com.ibios.core.schedule.TaskExecutionView;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class JpaScheduledTaskStore implements ScheduledTaskStore {

  private final ScheduledTaskRepository taskRepo;
  private final TaskExecutionRepository execRepo;

  public JpaScheduledTaskStore(ScheduledTaskRepository taskRepo, TaskExecutionRepository execRepo) {
    this.taskRepo = taskRepo;
    this.execRepo = execRepo;
  }

  @Override
  public void register(ScheduledTaskView task) {
    ScheduledTask entity = taskRepo.findById(task.taskId()).orElse(new ScheduledTask());
    entity.setTaskId(task.taskId());
    entity.setProfileName(task.profileName());
    entity.setCron(task.cron());
    entity.setZone(task.zone());
    entity.setMessage(task.message());
    entity.setEnabled(task.enabled());
    entity.setNextRunAt(task.nextRunAt());
    entity.setCreatedAt(task.createdAt());
    if (entity.getRunCount() == null) {
      entity.setRunCount(0);
    }
    taskRepo.save(entity);
  }

  @Override
  public void updateStatus(
      String taskId,
      java.time.Instant lastRunAt,
      String lastStatus,
      java.time.Instant nextRunAt,
      int runCount) {
    taskRepo
        .findById(taskId)
        .ifPresent(
            task -> {
              task.setLastRunAt(lastRunAt);
              task.setLastStatus(lastStatus);
              task.setNextRunAt(nextRunAt);
              task.setRunCount(runCount);
              taskRepo.save(task);
            });
  }

  @Override
  public void setEnabled(String taskId, boolean enabled) {
    taskRepo
        .findById(taskId)
        .ifPresent(
            task -> {
              task.setEnabled(enabled);
              taskRepo.save(task);
            });
  }

  @Override
  public ScheduledTaskView get(String taskId) {
    return taskRepo.findById(taskId).map(this::toView).orElse(null);
  }

  @Override
  public List<ScheduledTaskView> listAll() {
    return taskRepo.findAll().stream().map(this::toView).toList();
  }

  @Override
  public void recordExecution(TaskExecutionView execution) {
    TaskExecution entity = new TaskExecution();
    entity.setTaskId(execution.taskId());
    entity.setSessionId(execution.sessionId());
    entity.setStartedAt(execution.startedAt());
    entity.setSuccess(execution.success());
    entity.setErrorMessage(execution.errorMessage());
    entity.setDurationMs(execution.durationMs());
    execRepo.save(entity);
  }

  @Override
  public List<TaskExecutionView> getExecutions(String taskId) {
    return execRepo.findByTaskIdOrderByStartedAtDesc(taskId).stream()
        .map(this::toExecView)
        .toList();
  }

  private ScheduledTaskView toView(ScheduledTask e) {
    return new ScheduledTaskView(
        e.getTaskId(),
        e.getProfileName(),
        e.getCron(),
        e.getZone(),
        e.getMessage(),
        Boolean.TRUE.equals(e.getEnabled()),
        e.getNextRunAt(),
        e.getLastRunAt(),
        e.getLastStatus(),
        e.getRunCount() != null ? e.getRunCount() : 0,
        e.getCreatedAt());
  }

  private TaskExecutionView toExecView(TaskExecution e) {
    return new TaskExecutionView(
        e.getTaskId(),
        e.getSessionId(),
        e.getStartedAt(),
        Boolean.TRUE.equals(e.getSuccess()),
        e.getErrorMessage(),
        e.getDurationMs());
  }
}
