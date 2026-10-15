package com.ibios.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ibios.core.memory.MemoryScope;
import com.ibios.core.tool.IbiosTool;
import com.ibios.core.tool.ToolExecutionResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MemoryToolsTest {

  private InMemoryMem0Stub stub;
  private MemoryServiceImpl service;
  private MemoryTools memoryTools;

  @BeforeEach
  void setUp() {
    stub = new InMemoryMem0Stub();
    service = new MemoryServiceImpl(stub);
    memoryTools = new MemoryTools(service);
  }

  @Test
  @DisplayName("save_memory 缺省 scope 写归档")
  void saveMemoryDefaultScopeIsArchival() {
    IbiosTool saveTool = memoryTools.saveMemoryTool();
    ToolExecutionResult result = saveTool.execute("{\"content\": \"用户偏好简洁\"}");
    assertTrue(result.success());

    String context = service.buildContext(null);
    assertTrue(context.contains("用户偏好简洁"));

    assertEquals(
        1, stub.allEntries().stream().filter(e -> e.scope() == MemoryScope.ARCHIVAL).count());
    assertEquals(0, stub.allEntries().stream().filter(e -> e.scope() == MemoryScope.CORE).count());
  }

  @Test
  @DisplayName("save_memory 显式指定 core scope")
  void saveMemoryExplicitCoreScope() {
    IbiosTool saveTool = memoryTools.saveMemoryTool();
    ToolExecutionResult result = saveTool.execute("{\"content\": \"用户叫小王\", \"scope\": \"core\"}");
    assertTrue(result.success());

    assertEquals(1, stub.allEntries().stream().filter(e -> e.scope() == MemoryScope.CORE).count());
  }

  @Test
  @DisplayName("recall_memory 命中返回匹配结果")
  void recallMemoryWithHits() {
    stub.append("偏好简洁风格", MemoryScope.ARCHIVAL);
    stub.append("喜欢深色主题", MemoryScope.ARCHIVAL);

    IbiosTool recallTool = memoryTools.recallMemoryTool();
    ToolExecutionResult result = recallTool.execute("{\"keyword\": \"简洁\"}");

    assertTrue(result.success());
    assertTrue(result.output().contains("简洁"));
  }

  @Test
  @DisplayName("recall_memory 未命中返回'没有找到相关记忆'不抛异常")
  void recallMemoryNoMatchReturnsFriendlyMessage() {
    stub.append("某条记忆", MemoryScope.ARCHIVAL);

    IbiosTool recallTool = memoryTools.recallMemoryTool();
    ToolExecutionResult result = recallTool.execute("{\"keyword\": \"不存在的词\"}");

    assertTrue(result.success());
    assertEquals("没有找到相关记忆", result.output());
  }

  @Test
  @DisplayName("save_memory tool name 和 schema 正确")
  void saveMemoryToolMetadata() {
    IbiosTool tool = memoryTools.saveMemoryTool();
    assertEquals("save_memory", tool.name());
    assertTrue(tool.inputSchema().contains("content"));
    assertTrue(tool.inputSchema().contains("scope"));
  }

  @Test
  @DisplayName("recall_memory tool name 和 schema 正确")
  void recallMemoryToolMetadata() {
    IbiosTool tool = memoryTools.recallMemoryTool();
    assertEquals("recall_memory", tool.name());
    assertTrue(tool.inputSchema().contains("keyword"));
  }
}
