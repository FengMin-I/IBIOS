package com.ibios.provider;

import com.ibios.core.tool.IbiosTool;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.stereotype.Component;

@Component
public class ToolSchemaAdapter {

  public List<ToolCallback> adapt(List<IbiosTool> tools) {
    if (tools == null || tools.isEmpty()) {
      return Collections.emptyList();
    }
    List<ToolCallback> callbacks = new ArrayList<>(tools.size());
    for (IbiosTool tool : tools) {
      callbacks.add(new IbiosToolCallback(tool));
    }
    return callbacks;
  }

  private static final class IbiosToolCallback implements ToolCallback {

    private final ToolDefinition definition;

    IbiosToolCallback(IbiosTool tool) {
      this.definition =
          ToolDefinition.builder()
              .name(tool.name())
              .description(tool.description())
              .inputSchema(tool.inputSchema())
              .build();
    }

    @Override
    public ToolDefinition getToolDefinition() {
      return definition;
    }

    @Override
    public String call(String toolInput) {
      throw new UnsupportedOperationException("Schema-only callback; tool execution is disabled");
    }
  }
}
