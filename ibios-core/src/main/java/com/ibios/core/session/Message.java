package com.ibios.core.session;

public record Message(Role role, String content) {
  public enum Role {
    SYSTEM,
    USER,
    ASSISTANT,
    TOOL
  }
}
