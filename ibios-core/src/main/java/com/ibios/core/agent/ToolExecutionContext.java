package com.ibios.core.agent;

public class ToolExecutionContext {

  private static final ThreadLocal<String> AGENT_NAME = new ThreadLocal<>();

  public static void setAgentName(String name) {
    AGENT_NAME.set(name);
  }

  public static String getAgentName() {
    return AGENT_NAME.get();
  }

  public static void clear() {
    AGENT_NAME.remove();
  }
}
