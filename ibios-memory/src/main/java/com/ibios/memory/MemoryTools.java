package com.ibios.memory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibios.core.memory.MemoryScope;
import com.ibios.core.memory.MemoryService;
import com.ibios.core.tool.IbiosTool;
import com.ibios.core.tool.ToolExecutionResult;
import java.util.List;

public class MemoryTools {

  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final MemoryService memoryService;

  public MemoryTools(MemoryService memoryService) {
    this.memoryService = memoryService;
  }

  public IbiosTool saveMemoryTool() {
    return new SaveMemoryTool(memoryService);
  }

  public IbiosTool recallMemoryTool() {
    return new RecallMemoryTool(memoryService);
  }

  static class SaveMemoryTool implements IbiosTool {

    private final MemoryService memoryService;

    SaveMemoryTool(MemoryService memoryService) {
      this.memoryService = memoryService;
    }

    @Override
    public String name() {
      return "save_memory";
    }

    @Override
    public String description() {
      return "Save important information to long-term memory. "
          + "Use scope 'core' for always-present facts, 'archival' for searchable records.";
    }

    @Override
    public String inputSchema() {
      return "{\"type\":\"object\",\"properties\":{"
          + "\"content\":{\"type\":\"string\",\"description\":\"The content to remember\"},"
          + "\"scope\":{\"type\":\"string\",\"enum\":[\"core\",\"archival\"],"
          + "\"description\":\"Memory scope, defaults to archival\"}"
          + "},\"required\":[\"content\"]}";
    }

    @Override
    public ToolExecutionResult execute(String argumentsJson) {
      try {
        JsonNode args = OBJECT_MAPPER.readTree(argumentsJson);
        String content = args.get("content").asText();
        String scopeStr = args.has("scope") ? args.get("scope").asText() : "archival";
        MemoryScope scope =
            "core".equalsIgnoreCase(scopeStr) ? MemoryScope.CORE : MemoryScope.ARCHIVAL;
        memoryService.remember(content, scope);
        return ToolExecutionResult.success("Memory saved with scope: " + scope.name());
      } catch (Exception e) {
        return ToolExecutionResult.failure("Failed to save memory: " + e.getMessage(), false);
      }
    }
  }

  static class RecallMemoryTool implements IbiosTool {

    private final MemoryService memoryService;

    RecallMemoryTool(MemoryService memoryService) {
      this.memoryService = memoryService;
    }

    @Override
    public String name() {
      return "recall_memory";
    }

    @Override
    public String description() {
      return "Search archival memory by keyword to recall previously stored information.";
    }

    @Override
    public String inputSchema() {
      return "{\"type\":\"object\",\"properties\":{"
          + "\"keyword\":{\"type\":\"string\",\"description\":\"Keyword to search for\"}"
          + "},\"required\":[\"keyword\"]}";
    }

    @Override
    public ToolExecutionResult execute(String argumentsJson) {
      try {
        JsonNode args = OBJECT_MAPPER.readTree(argumentsJson);
        String keyword = args.get("keyword").asText();
        List<String> hits = memoryService.recall(keyword);
        if (hits.isEmpty()) {
          return ToolExecutionResult.success("没有找到相关记忆");
        }
        StringBuilder sb = new StringBuilder();
        for (String hit : hits) {
          sb.append("- ").append(hit).append("\n");
        }
        return ToolExecutionResult.success(sb.toString());
      } catch (Exception e) {
        return ToolExecutionResult.failure("Failed to recall memory: " + e.getMessage(), false);
      }
    }
  }
}
