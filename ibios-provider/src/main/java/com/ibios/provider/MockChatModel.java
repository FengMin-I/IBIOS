package com.ibios.provider;

import java.util.List;
import java.util.Map;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;

public class MockChatModel implements ChatModel {

  @Override
  public ChatResponse call(Prompt prompt) {
    boolean hasAssistantMessage = false;
    for (Message msg : prompt.getInstructions()) {
      if (msg instanceof AssistantMessage) {
        hasAssistantMessage = true;
        break;
      }
    }

    if (hasAssistantMessage) {
      AssistantMessage reply = new AssistantMessage("已记住，这是 mock 的确认答复。");
      return new ChatResponse(List.of(new Generation(reply)));
    }

    AssistantMessage.ToolCall toolCall =
        new AssistantMessage.ToolCall(
            "mock-call-1",
            "function",
            "save_memory",
            "{\"content\":\"mock 测试记忆\",\"scope\":\"core\"}");
    AssistantMessage withTool = new AssistantMessage("", Map.of(), List.of(toolCall));
    return new ChatResponse(List.of(new Generation(withTool)));
  }

  @Override
  public ChatOptions getDefaultOptions() {
    return ChatOptions.builder().model("mock").build();
  }
}
