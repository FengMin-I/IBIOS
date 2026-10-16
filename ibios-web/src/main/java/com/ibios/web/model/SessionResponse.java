package com.ibios.web.model;

import java.util.List;

public record SessionResponse(String sessionId, String profileName, List<MessageItem> messages) {

  public record MessageItem(String role, String content, String toolName) {}
}
