package com.ibios.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ibios.core.agent.ToolExecutionContext;
import com.ibios.core.memory.MemoryScope;
import com.ibios.core.session.Session;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MemoryServiceImplTest {

  @TempDir Path tempDir;

  private InMemoryMem0Stub stub;
  private MemoryServiceImpl service;

  @BeforeEach
  void setUp() {
    stub = new InMemoryMem0Stub();
    service = new MemoryServiceImpl(stub);
  }

  @Test
  @DisplayName("buildContext 返回长期记忆——核心全量+归档截断后")
  void buildContextReturnsLongTermMemory() {
    stub.append("核心偏好：简洁", MemoryScope.CORE);
    stub.append("归档：今天讨论了架构", MemoryScope.ARCHIVAL);
    Session session = new Session("s1", "default", "cli", "user1");

    String context = service.buildContext(session);

    assertTrue(context.contains("核心偏好：简洁"));
    assertTrue(context.contains("归档：今天讨论了架构"));
  }

  @Test
  @DisplayName("remember 写入核心记忆")
  void rememberCoreMemory() {
    service.remember("用户叫小王", MemoryScope.CORE);
    String context = service.buildContext(null);
    assertTrue(context.contains("用户叫小王"));
  }

  @Test
  @DisplayName("remember 写入归档记忆")
  void rememberArchivalMemory() {
    service.remember("今天天气好", MemoryScope.ARCHIVAL);
    String context = service.buildContext(null);
    assertTrue(context.contains("今天天气好"));
  }

  @Test
  @DisplayName("recall 检索归档记忆")
  void recallArchivalMemory() {
    stub.append("偏好简洁风格", MemoryScope.ARCHIVAL);
    stub.append("喜欢深色主题", MemoryScope.ARCHIVAL);

    List<String> hits = service.recall("简洁");

    assertEquals(1, hits.size());
    assertTrue(hits.get(0).contains("简洁"));
  }

  @Test
  @DisplayName("recall 未命中返回空列表")
  void recallNoMatchReturnsEmpty() {
    stub.append("某条记忆", MemoryScope.ARCHIVAL);
    List<String> hits = service.recall("不存在");
    assertTrue(hits.isEmpty());
  }

  @Test
  @DisplayName("remember 在 Agent 执行上下文中写入该 Agent 自己的 MEMORY.md")
  void rememberRoutesToAgentMemoryFile() throws IOException {
    String agentName = "test-memory-routing-" + UUID.randomUUID();
    Path agentDir = Path.of(".ibios/agents", agentName);
    MemoryServiceImpl markdownService =
        new MemoryServiceImpl(new MarkdownMemoryStore(Path.of(".ibios/memory/MEMORY.md")));
    Files.createDirectories(agentDir);
    ToolExecutionContext.setAgentName(agentName);
    try {
      markdownService.remember("执行成功", MemoryScope.ARCHIVAL);

      Path agentMemory = agentDir.resolve("MEMORY.md");
      assertTrue(Files.exists(agentMemory), "Agent memory file should be created");
      assertTrue(Files.readString(agentMemory).contains("执行成功"));
    } finally {
      ToolExecutionContext.clear();
      deleteRecursively(agentDir);
    }
  }

  @Test
  @DisplayName("Agent 目录存在时只用自己的记忆，不回退全局")
  void agentMemoryIsIsolatedFromGlobalStore() throws IOException {
    String agentName = "test-memory-isolation-" + UUID.randomUUID();
    Path agentDir = Path.of(".ibios/agents", agentName);
    Files.createDirectories(agentDir);
    Path globalFile = tempDir.resolve("global/MEMORY.md");
    MarkdownMemoryStore globalStore = new MarkdownMemoryStore(globalFile);
    globalStore.append("全局独有记忆", MemoryScope.ARCHIVAL);
    MemoryServiceImpl markdownService = new MemoryServiceImpl(globalStore);
    Session session = new Session("s1", agentName, "web", "user1");
    try {
      ToolExecutionContext.setAgentName(agentName);
      assertEquals("", markdownService.buildContext(session).strip());
      assertTrue(markdownService.recall("全局").isEmpty());
      ToolExecutionContext.clear();

      ToolExecutionContext.setAgentName(agentName);
      markdownService.remember("Agent 自己的记忆", MemoryScope.ARCHIVAL);
      ToolExecutionContext.clear();

      String context = markdownService.buildContext(session);
      assertTrue(context.contains("Agent 自己的记忆"));
      assertTrue(!context.contains("全局独有记忆"));
    } finally {
      ToolExecutionContext.clear();
      deleteRecursively(agentDir);
    }
  }

  private void deleteRecursively(Path dir) throws IOException {
    if (!Files.exists(dir)) {
      return;
    }
    try (Stream<Path> paths = Files.walk(dir)) {
      for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) {
        Files.deleteIfExists(path);
      }
    }
  }
}
