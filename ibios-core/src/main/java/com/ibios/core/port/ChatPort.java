package com.ibios.core.port;

import com.ibios.core.profile.Profile;
import com.ibios.core.session.Message;
import com.ibios.core.tool.IbiosTool;
import java.util.List;

public interface ChatPort {
  ChatResponse chat(String sessionId, Profile profile, Prompt prompt);

  record Prompt(String systemPrompt, List<Message> messages, List<IbiosTool> availableTools) {}

  record ChatResponse(String text, List<ToolCallResponse> toolCalls) {
    public record ToolCallResponse(String name, String argumentsJson) {}

    public boolean hasToolCalls() {
      return toolCalls != null && !toolCalls.isEmpty();
    }
  }
}
