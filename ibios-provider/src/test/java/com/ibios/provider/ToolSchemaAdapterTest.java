package com.ibios.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ibios.core.tool.IbiosTool;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

class ToolSchemaAdapterTest {

  private final ToolSchemaAdapter adapter = new ToolSchemaAdapter();

  @Test
  void shouldTranslateIbiosToolToToolCallback() {
    IbiosTool tool =
        new IbiosTool() {
          @Override
          public String name() {
            return "web_search";
          }

          @Override
          public String description() {
            return "Search the web";
          }

          @Override
          public String inputSchema() {
            return "{\"type\":\"object\",\"properties\":{\"query\":{\"type\":\"string\"}}}";
          }
        };

    List<ToolCallback> callbacks = adapter.adapt(List.of(tool));

    assertEquals(1, callbacks.size());
    ToolDefinition def = callbacks.get(0).getToolDefinition();
    assertEquals("web_search", def.name());
    assertEquals("Search the web", def.description());
    assertTrue(def.inputSchema().contains("query"));
  }

  @Test
  void shouldReturnEmptyForNullInput() {
    List<ToolCallback> result = adapter.adapt(null);
    assertTrue(result.isEmpty());
  }

  @Test
  void shouldReturnEmptyForEmptyList() {
    List<ToolCallback> result = adapter.adapt(Collections.emptyList());
    assertTrue(result.isEmpty());
  }

  @Test
  void shouldNotSupportExecution() {
    IbiosTool tool =
        new IbiosTool() {
          @Override
          public String name() {
            return "test";
          }

          @Override
          public String description() {
            return "test";
          }

          @Override
          public String inputSchema() {
            return "{}";
          }
        };

    List<ToolCallback> callbacks = adapter.adapt(List.of(tool));
    assertThrows(UnsupportedOperationException.class, () -> callbacks.get(0).call("{}"));
  }
}
