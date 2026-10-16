package com.ibios.core.schedule;

import java.time.Instant;
import java.util.List;

public interface ScheduledTaskStore {

  void register(ScheduledTaskView task);

  void updateStatus(
      String taskId, Instant lastRunAt, String lastStatus, Instant nextRunAt, int runCount);

  void setEnabled(String taskId, boolean enabled);

  ScheduledTaskView get(String taskId);

  List<ScheduledTaskView> listAll();

  void recordExecution(TaskExecutionView execution);

  List<TaskExecutionView> getExecutions(String taskId);
}
