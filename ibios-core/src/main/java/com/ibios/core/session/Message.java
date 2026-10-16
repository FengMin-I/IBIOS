package com.ibios.core.session;

public record Message(Role role, String content, String toolName) {

  public Message(Role role, String content) {
    this(role, content, null);
  }

  public enum Role {
    SYSTEM,
    USER,
    ASSISTANT,
    TOOL
  }
}
