package com.ibios.tool.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.ibios.core.tool.IbiosTool;
import com.ibios.core.tool.ToolExecutionResult;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ToolRegistryTest {

  private DefaultToolRegistry registry;

  @BeforeEach
  void setUp() {
    registry = new DefaultToolRegistry();
  }

  @Test
  void registerAndLookupTool() {
    IbiosTool tool = createTool("test_tool", "A test tool");
    registry.register(tool);

    IbiosTool found = registry.lookup("test_tool");
    assertNotNull(found);
    assertEquals("test_tool", found.name());
  }

  @Test
  void lookupReturnsNullForUnknownTool() {
    assertNull(registry.lookup("nonexistent"));
  }

  @Test
  void listAllReturnsAllRegisteredTools() {
    registry.register(createTool("tool_a", "Tool A"));
    registry.register(createTool("tool_b", "Tool B"));
    registry.register(createTool("tool_c", "Tool C"));

    List<IbiosTool> all = registry.listAll();
    assertEquals(3, all.size());
  }

  private IbiosTool createTool(String name, String description) {
    return new IbiosTool() {
      @Override
      public String name() {
        return name;
      }

      @Override
      public String description() {
        return description;
      }

      @Override
      public String inputSchema() {
        return "{\"type\":\"object\",\"properties\":{}}";
      }

      @Override
      public ToolExecutionResult execute(String argumentsJson) {
        return ToolExecutionResult.success("ok");
      }
    };
  }
}
