package com.ibios.core.session;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Session {
  private final String sessionId;
  private final String profileName;
  private final String channel;
  private final String userId;
  private final List<Message> messages;

  public Session(String sessionId, String profileName, String channel, String userId) {
    this.sessionId = sessionId;
    this.profileName = profileName;
    this.channel = channel;
    this.userId = userId;
    this.messages = new ArrayList<>();
  }

  public String id() {
    return sessionId;
  }

  public String profileName() {
    return profileName;
  }

  public String channel() {
    return channel;
  }

  public String userId() {
    return userId;
  }

  public List<Message> messages() {
    return Collections.unmodifiableList(messages);
  }

  public void appendUserMessage(String content) {
    messages.add(new Message(Message.Role.USER, content));
  }

  public void appendAssistantMessage(String content, List<ToolCall> toolCalls) {
    String toolName = (toolCalls != null && !toolCalls.isEmpty()) ? toolCalls.get(0).name() : null;
    messages.add(new Message(Message.Role.ASSISTANT, content, toolName));
  }

  public void appendToolResult(ToolResult result) {
    messages.add(new Message(Message.Role.TOOL, result.output(), result.toolName()));
  }

  public boolean hasToolCalls() {
    if (messages.isEmpty()) {
      return false;
    }
    Message last = messages.get(messages.size() - 1);
    return last.role() == Message.Role.ASSISTANT && last.toolName() != null;
  }

  public List<ToolCall> getToolCalls() {
    List<ToolCall> result = new ArrayList<>();
    for (Message msg : messages) {
      if (msg.role() == Message.Role.ASSISTANT && msg.toolName() != null) {
        result.add(new ToolCall(msg.toolName(), ""));
      }
    }
    return result;
  }
}
