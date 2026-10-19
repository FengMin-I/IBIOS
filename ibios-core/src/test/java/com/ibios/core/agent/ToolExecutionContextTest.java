package com.ibios.core.agent;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ToolExecutionContextTest {

  @AfterEach
  void tearDown() {
    ToolExecutionContext.clear();
  }

  @Test
  @DisplayName("set 和 get agent name")
  void setAndGetAgentName() {
    String agentName = "test-agent";

    ToolExecutionContext.setAgentName(agentName);
    String result = ToolExecutionContext.getAgentName();

    assertTrue(result.equals(agentName), "Agent name should match");
  }

  @Test
  @DisplayName("clear 移除 agent name")
  void clearRemovesAgentName() {
    ToolExecutionContext.setAgentName("test-agent");
    assertTrue(ToolExecutionContext.getAgentName() != null, "Agent name should be set");

    ToolExecutionContext.clear();
    String result = ToolExecutionContext.getAgentName();

    assertNull(result, "Agent name should be null after clear");
  }

  @Test
  @DisplayName("get 未设置时返回 null")
  void getReturnsNullWhenNotSet() {
    String result = ToolExecutionContext.getAgentName();

    assertNull(result, "Agent name should be null when not set");
  }
}
