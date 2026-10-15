package com.ibios.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ibios.core.memory.MemoryScope;
import com.ibios.core.session.Session;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MemoryServiceImplTest {

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
}
