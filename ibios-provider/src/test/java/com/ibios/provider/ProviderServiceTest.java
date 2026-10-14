package com.ibios.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ibios.core.port.ChatPort.ChatResponse;
import com.ibios.core.port.ChatPort.Prompt;
import com.ibios.core.profile.Profile;
import com.ibios.core.session.Message;
import com.ibios.core.tool.IbiosTool;
import com.ibios.storage.audit.LlmCallAudit;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.Generation;

@ExtendWith(MockitoExtension.class)
class ProviderServiceTest {

  @Mock private LlmCallAudit audit;

  @Mock private ToolSchemaAdapter toolSchemaAdapter;

  private ChatModel dashscopeModel;
  private ChatModel openaiModel;
  private ProviderService providerService;

  @BeforeEach
  void setUp() {
    dashscopeModel = mock(ChatModel.class);
    openaiModel = mock(ChatModel.class);

    ProviderChatModelFactory factory = mock(ProviderChatModelFactory.class);
    Map<String, ChatModel> models =
        Map.of(
            "dashscope", dashscopeModel,
            "openai", openaiModel);
    when(factory.getModels()).thenReturn(models);

    providerService = new ProviderService(factory, toolSchemaAdapter, audit);
  }

  @Test
  void shouldRouteToCorrectProvider() {
    Profile dashscopeProfile = createProfile("dashscope", "qwen-plus");
    Profile openaiProfile = createProfile("openai", "gpt-4");

    org.springframework.ai.chat.model.ChatResponse dashscopeResp =
        createChatResponse("Hello from DashScope");
    org.springframework.ai.chat.model.ChatResponse openaiResp =
        createChatResponse("Hello from OpenAI");

    when(dashscopeModel.call(any(org.springframework.ai.chat.prompt.Prompt.class)))
        .thenReturn(dashscopeResp);
    when(openaiModel.call(any(org.springframework.ai.chat.prompt.Prompt.class)))
        .thenReturn(openaiResp);

    Prompt prompt = new Prompt("system", List.of(new Message(Message.Role.USER, "Hi")), null);
    ChatResponse resp1 = providerService.chat("s1", dashscopeProfile, prompt);
    ChatResponse resp2 = providerService.chat("s2", openaiProfile, prompt);

    assertEquals("Hello from DashScope", resp1.text());
    assertEquals("Hello from OpenAI", resp2.text());

    verify(dashscopeModel).call(any(org.springframework.ai.chat.prompt.Prompt.class));
    verify(openaiModel).call(any(org.springframework.ai.chat.prompt.Prompt.class));
  }

  @Test
  void shouldRecordFailedAuditAndRethrow() {
    Profile profile = createProfile("dashscope", "qwen-plus");
    when(dashscopeModel.call(any(org.springframework.ai.chat.prompt.Prompt.class)))
        .thenThrow(new RuntimeException("API error"));

    Prompt prompt = new Prompt("system", List.of(new Message(Message.Role.USER, "Hi")), null);
    RuntimeException ex =
        assertThrows(RuntimeException.class, () -> providerService.chat("s1", profile, prompt));

    assertEquals("API error", ex.getMessage());

    ArgumentCaptor<Boolean> successCaptor = ArgumentCaptor.forClass(Boolean.class);
    ArgumentCaptor<String> errorCaptor = ArgumentCaptor.forClass(String.class);
    verify(audit)
        .record(
            eq("s1"),
            eq("dashscope"),
            eq("qwen-plus"),
            any(),
            successCaptor.capture(),
            errorCaptor.capture(),
            any(Long.class));
    assertEquals(false, successCaptor.getValue());
    assertEquals("API error", errorCaptor.getValue());
  }

  @Test
  void shouldPassToolCallbacksToChatModel() {
    Profile profile = createProfile("dashscope", "qwen-plus");

    IbiosTool mockTool =
        new IbiosTool() {
          @Override
          public String name() {
            return "test_tool";
          }

          @Override
          public String description() {
            return "A test tool";
          }

          @Override
          public String inputSchema() {
            return "{\"type\":\"object\",\"properties\":{\"q\":{\"type\":\"string\"}}}";
          }

          @Override
          public com.ibios.core.tool.ToolExecutionResult execute(String argumentsJson) {
            return com.ibios.core.tool.ToolExecutionResult.success("ok");
          }
        };

    when(toolSchemaAdapter.adapt(any())).thenReturn(List.of());
    when(dashscopeModel.call(any(org.springframework.ai.chat.prompt.Prompt.class)))
        .thenReturn(createChatResponse("done"));

    Prompt prompt =
        new Prompt(
            "system", List.of(new Message(Message.Role.USER, "use tool")), List.of(mockTool));
    providerService.chat("s1", profile, prompt);

    verify(toolSchemaAdapter).adapt(List.of(mockTool));
    verify(dashscopeModel).call(any(org.springframework.ai.chat.prompt.Prompt.class));
  }

  private Profile createProfile(String providerName, String model) {
    return new Profile(
        "test",
        "test profile",
        new Profile.Identity("agent", "prompt"),
        new Profile.ProviderConfig(providerName, model, null),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        Map.of(),
        10,
        20);
  }

  private org.springframework.ai.chat.model.ChatResponse createChatResponse(String text) {
    AssistantMessage message = new AssistantMessage(text);
    Generation generation = new Generation(message);
    return new org.springframework.ai.chat.model.ChatResponse(List.of(generation));
  }
}
