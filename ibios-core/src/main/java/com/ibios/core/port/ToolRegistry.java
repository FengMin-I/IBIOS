package com.ibios.core.port;

public interface ToolRegistry {
  IbiosToolLookup lookup(String name);

  interface IbiosToolLookup {
    String name();

    String description();

    String inputSchema();

    ToolExecutionResult execute(String argumentsJson);
  }

  record ToolExecutionResult(String output, boolean success, String errorMessage) {}
}
