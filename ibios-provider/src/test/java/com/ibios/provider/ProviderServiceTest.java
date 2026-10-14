package com.ibios.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ibios.core.profile.Profile;
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
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

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

    ChatResponse dashscopeResp = createChatResponse("Hello from DashScope");
    ChatResponse openaiResp = createChatResponse("Hello from OpenAI");

    when(dashscopeModel.call(any(Prompt.class))).thenReturn(dashscopeResp);
    when(openaiModel.call(any(Prompt.class))).thenReturn(openaiResp);

    ProviderResponse resp1 =
        providerService.chat("s1", dashscopeProfile, new ProviderRequest("Hi", null));
    ProviderResponse resp2 =
        providerService.chat("s2", openaiProfile, new ProviderRequest("Hi", null));

    assertEquals("Hello from DashScope", resp1.text());
    assertEquals("Hello from OpenAI", resp2.text());

    verify(dashscopeModel).call(any(Prompt.class));
    verify(openaiModel).call(any(Prompt.class));
  }

  @Test
  void shouldRecordFailedAuditAndRethrow() {
    Profile profile = createProfile("dashscope", "qwen-plus");
    when(dashscopeModel.call(any(Prompt.class))).thenThrow(new RuntimeException("API error"));

    RuntimeException ex =
        assertThrows(
            RuntimeException.class,
            () -> providerService.chat("s1", profile, new ProviderRequest("Hi", null)));

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
        };

    when(toolSchemaAdapter.adapt(any())).thenReturn(List.of());
    when(dashscopeModel.call(any(Prompt.class))).thenReturn(createChatResponse("done"));

    providerService.chat("s1", profile, new ProviderRequest("use tool", List.of(mockTool)));

    verify(toolSchemaAdapter).adapt(List.of(mockTool));
    verify(dashscopeModel).call(any(Prompt.class));
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
        Map.of());
  }

  private ChatResponse createChatResponse(String text) {
    AssistantMessage message = new AssistantMessage(text);
    Generation generation = new Generation(message);
    return new ChatResponse(List.of(generation));
  }
}
