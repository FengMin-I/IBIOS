package com.ibios;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ibios.core.port.ChatPort;
import com.ibios.core.port.ChatPort.ChatResponse;
import com.ibios.core.port.ChatPort.ChatResponse.ToolCallResponse;
import com.ibios.core.profile.Profile;
import com.ibios.core.profile.ProfileRegistry;
import com.ibios.provider.ProviderService;
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
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(classes = IbiosApplication.class)
@AutoConfigureMockMvc
@DisplayName("Mock Agent E2E 测试：@SpringBootTest 全链路 HTTP 端到端（无 key）")
class MockAgentE2ETest {

  @MockBean(name = "chatPort")
  private ChatPort chatPort;

  @MockBean private ProviderService providerService;

  @Autowired private MockMvc mockMvc;

  @Autowired private ProfileRegistry profileRegistry;

  private final AtomicBoolean firstCall = new AtomicBoolean(true);

  @BeforeEach
  void setUp() {
    if (profileRegistry.get("mock-agent") == null) {
      Profile profile =
          new Profile(
              "mock-agent",
              "Mock agent for E2E testing",
              new Profile.Identity("mock-bot", "You are a helpful test assistant."),
              new Profile.ProviderConfig("mock", "mock", null),
              List.of("save_memory", "recall_memory"),
              List.of(),
              List.of(),
              List.of(),
              List.of(),
              List.of(),
              List.of(),
              Map.of(),
              10,
              20,
              null);
      profileRegistry.register(profile);
    }

    firstCall.set(true);
    ChatResponse toolCallResponse =
        new ChatResponse(
            "",
            List.of(
                new ToolCallResponse(
                    "save_memory", "{\"content\":\"mock 测试记忆\",\"scope\":\"core\"}")));
    ChatResponse textResponse = new ChatResponse("已记住，这是 mock 的确认答复。", Collections.emptyList());

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
  @DisplayName("health 端点返回 ok")
  void healthEndpointReturnsOk() throws Exception {
    mockMvc
        .perform(get("/api/v1/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0));
  }

  @Test
  @DisplayName("tools 端点返回已注册工具列表（含 save_memory）")
  void toolsEndpointListsRegisteredTools() throws Exception {
    mockMvc
        .perform(get("/api/v1/tools"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThan(0)));
  }

  @Test
  @DisplayName("完整 HTTP 流程：建会话 → 发消息 → 查会话 → 列会话 → 查记忆")
  void fullHttpFlowCreateSessionSendMessageAndQuery() throws Exception {
    String createSessionJson = "{\"profileName\":\"mock-agent\",\"userId\":\"e2e-user\"}";

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/sessions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createSessionJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.sessionId").exists())
            .andExpect(jsonPath("$.data.profileName").value("mock-agent"))
            .andReturn();

    String createResponseJson = createResult.getResponse().getContentAsString();
    String sessionId = extractJsonField(createResponseJson, "sessionId");

    String sendMessageJson = "{\"content\":\"记住：我喜欢喝美式咖啡\"}";
    mockMvc
        .perform(
            post("/api/v1/sessions/" + sessionId + "/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(sendMessageJson))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.reply").exists());

    mockMvc
        .perform(get("/api/v1/sessions/" + sessionId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.sessionId").value(sessionId))
        .andExpect(jsonPath("$.data.messages").isArray())
        .andExpect(
            jsonPath("$.data.messages.length()").value(org.hamcrest.Matchers.greaterThan(0)));

    mockMvc
        .perform(get("/api/v1/sessions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThan(0)));

    mockMvc
        .perform(get("/api/v1/memory"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.content").exists());
  }

  @Test
  @DisplayName("profiles 端点返回已注册的 Profile 列表")
  void profilesEndpointListsRegisteredProfiles() throws Exception {
    mockMvc
        .perform(get("/api/v1/profiles"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data").isArray());
  }

  private String extractJsonField(String json, String field) {
    int keyIdx = json.indexOf("\"" + field + "\"");
    if (keyIdx < 0) {
      throw new IllegalArgumentException("Field not found: " + field);
    }
    int colonIdx = json.indexOf(':', keyIdx);
    int quoteStart = json.indexOf('"', colonIdx + 1);
    int quoteEnd = json.indexOf('"', quoteStart + 1);
    return json.substring(quoteStart + 1, quoteEnd);
  }
}
