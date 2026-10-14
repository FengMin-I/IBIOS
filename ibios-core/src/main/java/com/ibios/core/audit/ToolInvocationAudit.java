package com.ibios.core.audit;

public interface ToolInvocationAudit {
  void recordToolInvocation(
      String sessionId,
      String toolName,
      String inputJson,
      String outputJson,
      boolean success,
      String errorMessage,
      long durationMs);
}
