package com.ibios.core.react;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ibios.core.profile.Profile;
import com.ibios.core.profile.ProfileRegistry;
import com.ibios.core.schedule.ScheduledTaskStore;
import com.ibios.core.session.Session;
import com.ibios.core.session.SessionManager;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

@ExtendWith(MockitoExtension.class)
class AgentSchedulerTest {

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
  @DisplayName("CronTrigger 带上了配置的 cron 和时区")
  void cronTriggerIncludesExpressionAndZone() {
    Profile profile = createProfileWithSchedule("task-1", "0 0 9 * * *", "Asia/Shanghai", "hello");
    when(profileRegistry.getAll()).thenReturn(List.of(profile));
    when(taskScheduler.schedule(
            any(Runnable.class), any(org.springframework.scheduling.Trigger.class)))
        .thenReturn(org.mockito.Mockito.mock(java.util.concurrent.ScheduledFuture.class));

    scheduler.registerAll();

    ArgumentCaptor<org.springframework.scheduling.Trigger> triggerCaptor =
        ArgumentCaptor.forClass(org.springframework.scheduling.Trigger.class);
    verify(taskScheduler).schedule(any(Runnable.class), triggerCaptor.capture());

    Object captured = triggerCaptor.getValue();
    assert (captured instanceof CronTrigger);
    CronTrigger cronTrigger = (CronTrigger) captured;
    assert cronTrigger.getExpression().equals("0 0 9 * * *");
  }

  @Test
  @DisplayName("上一次还没跑完，本次触发直接跳过")
  void previousRunStillHoldingLock_skipsTrigger() throws Exception {
    Profile profile = createProfileWithSchedule("task-1", "0 0 9 * * *", "UTC", "hello");
    Profile.ScheduleConfig config = profile.schedules().get(0);

    Lock lock = scheduler.lockFor("task-1");
    lock.lock();
    try {
      Thread t = new Thread(() -> scheduler.runOnce(profile, config));
      t.start();
      t.join(1000);
      verify(agentService, never()).process(any(), any());
    } finally {
      lock.unlock();
    }
  }

  @Test
  @DisplayName("任务抛异常，不外抛且锁必须被释放")
  void exceptionInProcess_doesNotThrowAndLockReleased() {
    Profile profile = createProfileWithSchedule("task-1", "0 0 9 * * *", "UTC", "hello");
    Profile.ScheduleConfig config = profile.schedules().get(0);
    Session session = new Session("s1", profile.name(), "scheduler", "scheduler");
    when(sessionManager.getOrCreate("scheduler", "scheduler", profile.name())).thenReturn(session);
    when(agentService.process(any(), any())).thenThrow(new RuntimeException("boom"));

    assertDoesNotThrow(() -> scheduler.runOnce(profile, config));

    scheduler.runOnce(profile, config);
    verify(agentService, times(2)).process(any(), any());
  }

  @Test
  @DisplayName("会话身份固定为 scheduler/scheduler/profileName")
  void sessionIdentityFixedAs_scheduler_scheduler_profileName() {
    Profile profile = createProfileWithSchedule("task-1", "0 0 9 * * *", "UTC", "hello");
    Profile.ScheduleConfig config = profile.schedules().get(0);
    Session session = new Session("s1", profile.name(), "scheduler", "scheduler");
    when(sessionManager.getOrCreate("scheduler", "scheduler", profile.name())).thenReturn(session);
    when(agentService.process(any(), any())).thenReturn("ok");

    scheduler.runOnce(profile, config);
    scheduler.runOnce(profile, config);

    verify(sessionManager, times(2)).getOrCreate("scheduler", "scheduler", profile.name());
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
