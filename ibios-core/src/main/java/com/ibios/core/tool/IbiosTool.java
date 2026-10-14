package com.ibios.core.tool;

public interface IbiosTool {

  String name();

  String description();

  String inputSchema();

  ToolExecutionResult execute(String argumentsJson);
}
