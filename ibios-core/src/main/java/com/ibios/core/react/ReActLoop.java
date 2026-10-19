package com.ibios.core.react;

import com.ibios.core.port.ChatPort;
import com.ibios.core.port.ChatPort.ChatResponse;
import com.ibios.core.port.ChatPort.ChatResponse.ToolCallResponse;
import com.ibios.core.port.ChatPort.Prompt;
import com.ibios.core.profile.Profile;
import com.ibios.core.session.Session;
import com.ibios.core.session.ToolCall;
import com.ibios.core.session.ToolResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ReActLoop {
  private static final Logger log = LoggerFactory.getLogger(ReActLoop.class);

  private final PromptBuilder promptBuilder;
  private final ChatPort chatPort;
  private final ToolExecutor toolExecutor;

  public ReActLoop(PromptBuilder promptBuilder, ChatPort chatPort, ToolExecutor toolExecutor) {
    this.promptBuilder = promptBuilder;
    this.chatPort = chatPort;
    this.toolExecutor = toolExecutor;
  }

  public String run(Session session, String userMessage, Profile profile) {
    session.appendUserMessage(userMessage);

    for (int i = 0; i < profile.maxIterations(); i++) {
      Prompt prompt = promptBuilder.build(session, profile);
      ChatResponse response = chatPort.chat(session.id(), profile, prompt);

      session.appendAssistantMessage(response.text(), convertToolCalls(response));

      if (!response.hasToolCalls()) {
        return response.text();
      }

      log.info("Executing {} tool calls", response.toolCalls().size());
      for (ToolCallResponse toolCall : response.toolCalls()) {
        log.info("Executing tool: {} with args: {}", toolCall.name(), toolCall.argumentsJson());
        ToolResult result =
            toolExecutor.execute(
                session.id(), profile.name(), toolCall.name(), toolCall.argumentsJson());
        log.info("Tool {} execution result: success={}", toolCall.name(), result.success());
        session.appendToolResult(result);
      }
    }

    return "达到最大轮数，已停止";
  }

  private List<ToolCall> convertToolCalls(ChatResponse response) {
    if (!response.hasToolCalls()) {
      return Collections.emptyList();
    }
    List<ToolCall> result = new ArrayList<>();
    for (ToolCallResponse tc : response.toolCalls()) {
      result.add(new ToolCall(tc.name(), tc.argumentsJson()));
    }
    return result;
  }
}
