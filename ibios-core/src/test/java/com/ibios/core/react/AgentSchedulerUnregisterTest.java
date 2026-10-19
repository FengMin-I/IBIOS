package com.ibios.core.react;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ibios.core.profile.Profile;
import com.ibios.core.profile.ProfileRegistry;
import com.ibios.core.schedule.ScheduledTaskStore;
import com.ibios.core.session.SessionManager;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;

@ExtendWith(MockitoExtension.class)
class AgentSchedulerUnregisterTest {

  @Mock private TaskScheduler taskScheduler;
  @Mock private ProfileRegistry profileRegistry;
  @Mock private AgentService agentService;
  @Mock private SessionManager sessionManager;
  @Mock private ScheduledTaskStore taskStore;

  private AgentScheduler scheduler;

  @BeforeEach
  void setUp() {
    scheduler =
        new AgentScheduler(taskScheduler, profileRegistry, agentService, sessionManager, taskStore);
  }

  @Test
  @DisplayName("unregisterProfile 为每个 schedule 取消 ScheduledFuture")
  void unregisterProfileCancelsScheduledFutureForEachSchedule() {
    Profile profile = createProfileWithSchedule("task-1", "0 0 9 * * *", "UTC", "hello");
    ScheduledFuture<?> mockFuture = mock(ScheduledFuture.class);
    doReturn(mockFuture)
        .when(taskScheduler)
        .schedule(any(Runnable.class), any(org.springframework.scheduling.Trigger.class));

    scheduler.registerProfile(profile);
    scheduler.unregisterProfile(profile);

    verify(mockFuture).cancel(false);
  }

  @Test
  @DisplayName("unregisterProfile 无 schedule 时不做任何操作")
  void unregisterProfileWithNoSchedulesDoesNothing() {
    Profile profile =
        new Profile(
            "test-profile",
            "test profile",
            new Profile.Identity("agent", "prompt"),
            new Profile.ProviderConfig("dashscope", "qwen-plus", null),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            Map.of(),
            10,
            20,
            null);

    scheduler.unregisterProfile(profile);

    Map<String, ScheduledFuture<?>> tasks = scheduler.scheduledTasks();
    assertTrue(tasks.isEmpty(), "No tasks should be registered");
  }

  @Test
  @DisplayName("unregisterProfile 从 scheduledTasks map 中移除 handle")
  void unregisterProfileRemovesHandleFromScheduledTasksMap() {
    Profile profile = createProfileWithSchedule("task-1", "0 0 9 * * *", "UTC", "hello");
    ScheduledFuture<?> mockFuture = mock(ScheduledFuture.class);
    doReturn(mockFuture)
        .when(taskScheduler)
        .schedule(any(Runnable.class), any(org.springframework.scheduling.Trigger.class));

    scheduler.registerProfile(profile);
    Map<String, ScheduledFuture<?>> tasksBefore = scheduler.scheduledTasks();
    assertTrue(tasksBefore.containsKey("task-1"), "Task should be registered");

    scheduler.unregisterProfile(profile);
    Map<String, ScheduledFuture<?>> tasksAfter = scheduler.scheduledTasks();

    assertFalse(tasksAfter.containsKey("task-1"), "Task should be removed from map");
  }

  private Profile createProfileWithSchedule(String id, String cron, String zone, String message) {
    Profile.ScheduleConfig scheduleConfig = new Profile.ScheduleConfig(id, cron, zone, message);
    return new Profile(
        "test-profile",
        "test profile",
        new Profile.Identity("agent", "prompt"),
        new Profile.ProviderConfig("dashscope", "qwen-plus", null),
        List.of(),
        List.of(),
        List.of(),
        List.of(scheduleConfig),
        List.of(),
        Map.of(),
        10,
        20,
        null);
  }
}
