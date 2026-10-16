package com.ibios;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.ibios.core.memory.MemoryService;
import com.ibios.core.port.ToolRegistry;
import com.ibios.core.profile.ProfileRegistry;
import com.ibios.core.react.AgentService;
import com.ibios.core.session.Session;
import com.ibios.core.session.SessionManager;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = IbiosApplication.class)
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:sqlite:file:human_trigger_flow_it?mode=memory&cache=shared",
      "spring.datasource.driver-class-name=org.sqlite.JDBC",
      "spring.jpa.hibernate.ddl-auto=update",
    })
@Tag("integration")
@DisplayName("人推链路全流程集成测试（需真 key，CI 默认跳过）")
class HumanTriggerFlowIT {

  @Autowired private AgentService agentService;
  @Autowired private SessionManager sessionManager;
  @Autowired private ProfileRegistry profileRegistry;
  @Autowired private MemoryService memoryService;
  @Autowired private ToolRegistry toolRegistry;

  @BeforeAll
  static void checkApiKey() {
    String key = System.getenv("DASHSCOPE_API_KEY");
    assumeTrue(key != null && !key.isBlank(), "DASHSCOPE_API_KEY 未设置，跳过集成测试");
  }

  @Test
  @DisplayName("支柱一：对话答得出——主流程跑通，审计正确")
  void conversationFlowProducesReplyAndAudit() {
    Session session = sessionManager.getOrCreate("cli", "test-user", "default");
    String reply = agentService.process(session, "你好，请简单介绍一下你自己");

    assertNotNull(reply, "回复不应为空");
    assertFalse(reply.isBlank(), "回复不应为空白");

    List<Session> sessions = sessionManager.listAll();
    assertFalse(sessions.isEmpty(), "会话列表不应为空");
  }

  @Test
  @DisplayName("支柱三：工具查得到——工具注册表非空，含关键工具")
  void toolRegistryContainsExpectedTools() {
    var tools = toolRegistry.listAll();
    assertFalse(tools.isEmpty(), "工具注册表不应为空");

    boolean hasSaveMemory = tools.stream().anyMatch(t -> "save_memory".equals(t.name()));
    assertTrue(hasSaveMemory, "工具注册表应包含 save_memory");
  }

  @Test
  @DisplayName("支柱二：记忆查得到——写得进、读得出")
  void memoryWriteAndRead() {
    String testContent = "集成测试记忆：" + System.currentTimeMillis();
    memoryService.remember(testContent, com.ibios.core.memory.MemoryScope.ARCHIVAL);

    List<String> hits = memoryService.recall("集成测试记忆");
    assertFalse(hits.isEmpty(), "应能检索到刚写入的记忆");
    assertTrue(hits.stream().anyMatch(h -> h.contains("集成测试记忆")), "检索结果应包含写入的内容");
  }
}
