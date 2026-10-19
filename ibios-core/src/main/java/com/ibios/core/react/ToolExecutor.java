package com.ibios.core.react;

import com.ibios.core.agent.ToolExecutionContext;
import com.ibios.core.audit.ToolInvocationAudit;
import com.ibios.core.port.ToolRegistry;
import com.ibios.core.session.ToolResult;
import com.ibios.core.tool.IbiosTool;
import com.ibios.core.tool.ToolExecutionResult;

public class ToolExecutor {
  private final ToolRegistry toolRegistry;
  private final ToolInvocationAudit audit;

  public ToolExecutor(ToolRegistry toolRegistry, ToolInvocationAudit audit) {
    this.toolRegistry = toolRegistry;
    this.audit = audit;
  }

  public ToolResult execute(String sessionId, String toolName, String argumentsJson) {
    long startTime = System.currentTimeMillis();

    IbiosTool tool = toolRegistry.lookup(toolName);
    if (tool == null) {
      long duration = System.currentTimeMillis() - startTime;
      String errorMsg = "Tool not found: " + toolName;
      audit.recordToolInvocation(
          sessionId, toolName, argumentsJson, null, false, errorMsg, duration);
      return new ToolResult(toolName, argumentsJson, null, false, errorMsg);
    }

    try {
      ToolExecutionResult result = tool.execute(argumentsJson);
      long duration = System.currentTimeMillis() - startTime;

      audit.recordToolInvocation(
          sessionId,
          toolName,
          argumentsJson,
          result.output(),
          result.success(),
          result.errorMessage(),
          duration);

      return new ToolResult(
          toolName, argumentsJson, result.output(), result.success(), result.errorMessage());
    } catch (Exception e) {
      long duration = System.currentTimeMillis() - startTime;
      String errorMsg = e.getMessage();
      audit.recordToolInvocation(
          sessionId, toolName, argumentsJson, null, false, errorMsg, duration);
      return new ToolResult(toolName, argumentsJson, null, false, errorMsg);
    }
  }

  public ToolResult execute(
      String sessionId, String profileName, String toolName, String argumentsJson) {
    ToolExecutionContext.setAgentName(profileName);
    try {
      return execute(sessionId, toolName, argumentsJson);
    } finally {
      ToolExecutionContext.clear();
    }
  }
}
