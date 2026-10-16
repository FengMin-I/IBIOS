package com.ibios.web.exception;

public class AgentNotFoundException extends RuntimeException {

  public AgentNotFoundException(String agentName) {
    super("Agent not found: " + agentName);
  }
}
