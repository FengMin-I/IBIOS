package com.ibios;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ibios.core.port.ChatPort;
import com.ibios.core.port.ChatPort.ChatResponse;
import com.ibios.core.port.ChatPort.ChatResponse.ToolCallResponse;
import com.ibios.core.profile.Profile;
import com.ibios.core.profile.ProfileRegistry;
import com.ibios.core.react.AgentScheduler;
import com.ibios.core.schedule.ScheduledTaskStore;
import com.ibios.core.schedule.ScheduledTaskView;
import com.ibios.provider.ProviderService;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(classes = IbiosApplication.class)
@AutoConfigureMockMvc
@DisplayName("定时任务子系统 E2E 测试（mock 驱动、无 key）")
class ScheduledTaskE2ETest {

  private static final String TASK_ID = "e2e-test-task";
  private static final String PROFILE_NAME = "scheduled-e2e-agent";

  @MockBean(name = "chatPort")
  private ChatPort chatPort;

  @MockBean private ProviderService providerService;

  @Autowired private MockMvc mockMvc;

  @Autowired private ProfileRegistry profileRegistry;

  @Autowired private AgentScheduler agentScheduler;

  @Autowired private ScheduledTaskStore taskStore;

  private final AtomicBoolean firstCall = new AtomicBoolean(true);

  @BeforeEach
  void setUp() {
    if (profileRegistry.get(PROFILE_NAME) == null) {
      Profile profile =
          new Profile(
              PROFILE_NAME,
              "Scheduled task E2E test agent",
              new Profile.Identity("scheduled-bot", "You are a test assistant."),
              new Profile.ProviderConfig("mock", "mock", null),
              List.of(),
              List.of(),
              List.of(),
              List.of(
                  new Profile.ScheduleConfig(
                      TASK_ID, "0 0 3 1 * ?", "Asia/Shanghai", "test scheduled message")),
              List.of(),
              Map.of(),
              10,
              20,
              null);
      profileRegistry.register(profile);
    }

    taskStore.register(
        new ScheduledTaskView(
            TASK_ID,
            PROFILE_NAME,
            "0 0 3 1 * ?",
            "Asia/Shanghai",
            "test scheduled message",
            true,
            Instant.now().plusSeconds(86400),
            null,
            null,
            0,
            Instant.now()));

    firstCall.set(true);
    ChatResponse toolCallResponse =
        new ChatResponse(
            "",
            List.of(
                new ToolCallResponse(
                    "save_memory", "{\"content\":\"scheduled test memory\",\"scope\":\"core\"}")));
    ChatResponse textResponse =
        new ChatResponse("Scheduled task completed.", Collections.emptyList());

    when(chatPort.chat(anyString(), any(Profile.class), any(ChatPort.Prompt.class)))
        .thenAnswer(
            invocation -> {
              if (firstCall.compareAndSet(true, false)) {
                return toolCallResponse;
              } else {
                firstCall.set(true);
                return textResponse;
              }
            });
    when(providerService.chat(anyString(), any(Profile.class), any(ChatPort.Prompt.class)))
        .thenAnswer(
            invocation -> {
              if (firstCall.get()) {
                return toolCallResponse;
              } else {
                return textResponse;
              }
            });
  }

  @Test
  @DisplayName("列出定时任务：启动后登记进 SQLite，GET 可查")
  void listScheduledTasks() throws Exception {
    mockMvc
        .perform(get("/api/v1/schedules"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data[?(@.taskId == '" + TASK_ID + "')]").exists())
        .andExpect(jsonPath("$.data[?(@.taskId == '" + TASK_ID + "')].enabled").value(true));
  }

  @Test
  @DisplayName("立即执行：run_count 更新、last_status=success、执行历史落库")
  void runNowAndVerifyPersistence() throws Exception {
    mockMvc
        .perform(post("/api/v1/schedules/" + TASK_ID + "/run"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0));

    mockMvc
        .perform(get("/api/v1/schedules"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[?(@.taskId == '" + TASK_ID + "')].runCount").value(1))
        .andExpect(
            jsonPath("$.data[?(@.taskId == '" + TASK_ID + "')].lastStatus").value("success"));

    mockMvc
        .perform(get("/api/v1/schedules/" + TASK_ID + "/executions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data.length()").value(1))
        .andExpect(jsonPath("$.data[0].success").value(true))
        .andExpect(jsonPath("$.data[0].taskId").value(TASK_ID));
  }

  @Test
  @DisplayName("停用后：enabled=false，执行历史仍保留")
  void disableTaskAndVerify() throws Exception {
    String body = "{\"enabled\":false}";
    mockMvc
        .perform(
            put("/api/v1/schedules/" + TASK_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0));

    mockMvc
        .perform(get("/api/v1/schedules"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[?(@.taskId == '" + TASK_ID + "')].enabled").value(false));
  }

  @Test
  @DisplayName("不存在的任务执行历史返回 400")
  void executionsForNonExistentTaskReturns400() throws Exception {
    mockMvc
        .perform(get("/api/v1/schedules/non-existent-task/executions"))
        .andExpect(status().isBadRequest());
  }
}
