package com.ibios.core.tool;

public record ToolExecutionResult(
    String output, boolean success, String errorMessage, boolean retryable) {

  public static ToolExecutionResult success(String output) {
    return new ToolExecutionResult(output, true, null, false);
  }

  public static ToolExecutionResult failure(String errorMessage, boolean retryable) {
    return new ToolExecutionResult(null, false, errorMessage, retryable);
  }
}
