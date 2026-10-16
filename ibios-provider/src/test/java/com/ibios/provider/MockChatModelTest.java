package com.ibios.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;

@DisplayName("MockChatModel 脚本行为测试")
class MockChatModelTest {

  private final MockChatModel mockChatModel = new MockChatModel();

  @Test
  @DisplayName("首轮调用（无 AssistantMessage）返回 save_memory 工具调用")
  void firstCallReturnsToolCall() {
    Prompt prompt = new Prompt(List.of(new UserMessage("记住：我喜欢喝美式咖啡")));

    ChatResponse response = mockChatModel.call(prompt);

    assertNotNull(response);
    assertNotNull(response.getResult());
    AssistantMessage output = response.getResult().getOutput();
    assertTrue(output.hasToolCalls());
    assertEquals(1, output.getToolCalls().size());

    AssistantMessage.ToolCall toolCall = output.getToolCalls().get(0);
    assertEquals("mock-call-1", toolCall.id());
    assertEquals("function", toolCall.type());
    assertEquals("save_memory", toolCall.name());
    assertTrue(toolCall.arguments().contains("mock 测试记忆"));
    assertTrue(toolCall.arguments().contains("core"));
  }

  @Test
  @DisplayName("二轮调用（含 AssistantMessage）返回纯文本确认")
  void secondCallReturnsTextReply() {
    Prompt prompt =
        new Prompt(
            List.of(
                new UserMessage("记住：我喜欢喝美式咖啡"),
                new AssistantMessage(""),
                new UserMessage("tool result: Memory saved")));

    ChatResponse response = mockChatModel.call(prompt);

    assertNotNull(response);
    assertNotNull(response.getResult());
    AssistantMessage output = response.getResult().getOutput();
    assertFalse(output.hasToolCalls());
    assertEquals("已记住，这是 mock 的确认答复。", output.getText());
  }

  @Test
  @DisplayName("getDefaultOptions 返回 model=mock")
  void defaultOptionsReturnMockModel() {
    ChatOptions options = mockChatModel.getDefaultOptions();
    assertNotNull(options);
    assertEquals("mock", options.getModel());
  }
}
