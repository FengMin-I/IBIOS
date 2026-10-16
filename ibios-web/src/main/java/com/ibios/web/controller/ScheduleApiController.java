package com.ibios.web.controller;

import com.ibios.core.react.AgentScheduler;
import com.ibios.core.schedule.ScheduledTaskStore;
import com.ibios.core.schedule.ScheduledTaskView;
import com.ibios.core.schedule.TaskExecutionView;
import com.ibios.web.model.ApiResponse;
import com.ibios.web.model.ScheduleTaskResponse;
import com.ibios.web.model.TaskExecutionResponse;
import com.ibios.web.model.UpdateScheduleRequest;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/schedules")
public class ScheduleApiController {

  private final AgentScheduler agentScheduler;

  public ScheduleApiController(AgentScheduler agentScheduler) {
    this.agentScheduler = agentScheduler;
  }

  @GetMapping
  public ApiResponse<List<ScheduleTaskResponse>> list() {
    ScheduledTaskStore store = agentScheduler.taskStore();
    List<ScheduleTaskResponse> list = store.listAll().stream().map(this::toResponse).toList();
    return ApiResponse.ok(list);
  }

  @GetMapping("/{id}/executions")
  public ApiResponse<List<TaskExecutionResponse>> executions(@PathVariable String id) {
    ScheduledTaskStore store = agentScheduler.taskStore();
    ScheduledTaskView task = store.get(id);
    if (task == null) {
      throw new IllegalArgumentException("Scheduled task not found: " + id);
    }
    List<TaskExecutionResponse> list =
        store.getExecutions(id).stream().map(this::toExecResponse).toList();
    return ApiResponse.ok(list);
  }

  @PostMapping("/{id}/run")
  public ApiResponse<Void> runNow(@PathVariable String id) {
    agentScheduler.runNow(id);
    return ApiResponse.ok();
  }

  @PutMapping("/{id}")
  public ApiResponse<Void> update(
      @PathVariable String id, @RequestBody UpdateScheduleRequest request) {
    ScheduledTaskStore store = agentScheduler.taskStore();
    ScheduledTaskView task = store.get(id);
    if (task == null) {
      throw new IllegalArgumentException("Scheduled task not found: " + id);
    }
    store.setEnabled(id, request.isEnabled());
    return ApiResponse.ok();
  }

  private ScheduleTaskResponse toResponse(ScheduledTaskView v) {
    ScheduleTaskResponse r = new ScheduleTaskResponse();
    r.setTaskId(v.taskId());
    r.setProfileName(v.profileName());
    r.setCron(v.cron());
    r.setZone(v.zone());
    r.setMessage(v.message());
    r.setEnabled(v.enabled());
    r.setNextRunAt(v.nextRunAt());
    r.setLastRunAt(v.lastRunAt());
    r.setLastStatus(v.lastStatus());
    r.setRunCount(v.runCount());
    r.setCreatedAt(v.createdAt());
    return r;
  }

  private TaskExecutionResponse toExecResponse(TaskExecutionView v) {
    TaskExecutionResponse r = new TaskExecutionResponse();
    r.setTaskId(v.taskId());
    r.setSessionId(v.sessionId());
    r.setStartedAt(v.startedAt());
    r.setSuccess(v.success());
    r.setErrorMessage(v.errorMessage());
    r.setDurationMs(v.durationMs());
    return r;
  }
}
