package com.ibios.storage.audit;

public interface LlmCallAudit {

  void record(
      String sessionId,
      String provider,
      String model,
      TokenUsage usage,
      boolean success,
      String errorMessage,
      long latencyMs);

  record TokenUsage(int promptTokens, int completionTokens, int totalTokens) {}
}
