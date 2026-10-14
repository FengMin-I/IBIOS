package com.ibios.tool.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ibios.core.tool.ToolExecutionResult;
import org.junit.jupiter.api.Test;

class McpToolAdapterTest {

  @Test
  void adapterShouldMapNameDescriptionSchema() {
    McpToolAdapter adapter =
        new McpToolAdapter(
            "test-server",
            "test_tool",
            "A test tool from MCP",
            "{\"type\":\"object\",\"properties\":{}}");

    assertEquals("test_tool", adapter.name());
    assertEquals("A test tool from MCP", adapter.description());
    assertNotNull(adapter.inputSchema());
  }

  @Test
  void adapterExecuteShouldReturnRetryableFailure() {
    McpToolAdapter adapter = new McpToolAdapter("test-server", "test_tool", "desc", "{}");

    ToolExecutionResult result = adapter.execute("{}");

    assertFalse(result.success());
    assertTrue(result.retryable(), "MCP tool failures should be retryable");
    assertNotNull(result.errorMessage());
  }
}
