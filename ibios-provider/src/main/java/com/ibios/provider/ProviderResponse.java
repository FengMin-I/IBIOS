package com.ibios.provider;

import java.util.List;

public record ProviderResponse(String text, List<ToolCall> toolCalls, TokenUsage usage) {

  public record ToolCall(String name, String argumentsJson) {}

  public record TokenUsage(int promptTokens, int completionTokens, int totalTokens) {}
}
