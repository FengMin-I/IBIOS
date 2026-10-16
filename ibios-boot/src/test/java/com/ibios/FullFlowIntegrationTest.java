package com.ibios;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ibios.core.profile.Profile;
import com.ibios.core.profile.ProfileRegistry;
import com.ibios.storage.audit.LlmCall;
import com.ibios.storage.audit.LlmCallRepository;
import com.ibios.storage.audit.ToolInvocation;
import com.ibios.storage.audit.ToolInvocationRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(classes = IbiosApplication.class)
@AutoConfigureMockMvc
@DisplayName("全流程集成测试：启动服务 → 发起对话 → 对账 session / 记忆 / 审计")
class FullFlowIntegrationTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ProfileRegistry profileRegistry;

  @Autowired private LlmCallRepository llmCallRepository;

  @Autowired private ToolInvocationRepository toolInvocationRepository;

  @BeforeEach
  void setUp() {
    if (profileRegistry.get("mock-agent") == null) {
      Profile profile =
          new Profile(
              "mock-agent",
              "Mock agent for integration testing",
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
  }

  @Test
  @DisplayName("完整对话流程：建会话 → 发消息 → 查 session / 记忆 / tool 调用 / llm 审计")
  void fullConversationFlowWithAuditVerification() throws Exception {
    String createSessionJson = "{\"profileName\":\"mock-agent\",\"userId\":\"integration-user\"}";

    MvcResult createResult =
        mockMvc
            .perform(
                post("/api/v1/sessions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(createSessionJson))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.sessionId").exists())
            .andReturn();

    String createResponseJson = createResult.getResponse().getContentAsString();
    String sessionId = extractJsonField(createResponseJson, "sessionId");
    assertNotNull(sessionId);
    assertFalse(sessionId.isBlank());

    String sendMessageJson = "{\"content\":\"记住：我喜欢喝美式咖啡\"}";
    mockMvc
        .perform(
            post("/api/v1/sessions/" + sessionId + "/messages")
                .contentType(MediaType.APPLICATION_JSON)
                .content(sendMessageJson))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(0))
        .andExpect(jsonPath("$.data.reply").value("已记住，这是 mock 的确认答复。"));

    MvcResult sessionResult =
        mockMvc
            .perform(get("/api/v1/sessions/" + sessionId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value(0))
            .andExpect(jsonPath("$.data.sessionId").value(sessionId))
            .andExpect(jsonPath("$.data.messages").isArray())
            .andReturn();

    String sessionJson = sessionResult.getResponse().getContentAsString();
    assertTrue(sessionJson.contains("USER"), "会话历史应包含 USER 消息");
    assertTrue(sessionJson.contains("ASSISTANT"), "会话历史应包含 ASSISTANT 消息");

    mockMvc
        .perform(get("/api/v1/sessions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThan(0)));

    mockMvc
        .perform(get("/api/v1/memory"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.content").exists());

    mockMvc
        .perform(get("/api/v1/tools"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data.length()").value(org.hamcrest.Matchers.greaterThan(0)));

    List<LlmCall> llmCalls = llmCallRepository.findAll();
    assertFalse(llmCalls.isEmpty(), "llm_calls 审计表应有记录");
    assertTrue(
        llmCalls.stream().anyMatch(c -> sessionId.equals(c.getSessionId())), "llm_calls 应有本次会话的记录");
    assertTrue(llmCalls.stream().allMatch(LlmCall::getSuccess), "llm_calls 应全部成功");

    List<ToolInvocation> toolInvocations = toolInvocationRepository.findAll();
    assertFalse(toolInvocations.isEmpty(), "tool_invocations 审计表应有记录");
    assertTrue(
        toolInvocations.stream().anyMatch(t -> sessionId.equals(t.getSessionId())),
        "tool_invocations 应有本次会话的记录");
    assertTrue(
        toolInvocations.stream()
            .anyMatch(t -> "save_memory".equals(t.getToolName()) && t.getSuccess()),
        "应有 save_memory 成功调用记录");

    long llmCountForSession =
        llmCalls.stream().filter(c -> sessionId.equals(c.getSessionId())).count();
    assertEquals(2, llmCountForSession, "本次会话应有 2 次 LLM 调用（两轮 ReAct）");

    long toolCountForSession =
        toolInvocations.stream().filter(t -> sessionId.equals(t.getSessionId())).count();
    assertEquals(1, toolCountForSession, "本次会话应有 1 次工具调用（save_memory）");
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
