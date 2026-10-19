package com.ibios.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ibios.core.audit.ToolInvocationAudit;
import com.ibios.core.memory.MemoryScope;
import com.ibios.core.port.ChatPort;
import com.ibios.core.port.ChatPort.ChatResponse;
import com.ibios.core.port.ChatPort.ChatResponse.ToolCallResponse;
import com.ibios.core.port.ToolRegistry;
import com.ibios.core.profile.Profile;
import com.ibios.core.react.ContextLoader;
import com.ibios.core.react.PromptBuilder;
import com.ibios.core.react.ReActLoop;
import com.ibios.core.react.ToolExecutor;
import com.ibios.core.session.Message;
import com.ibios.core.session.Session;
import com.ibios.core.tool.IbiosTool;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Mock Provider 全链路-flow 测试：手工装配 ReAct + mock ChatPort + 真实工具/记忆/会话")
class MockProviderFlowTest {

  private InMemoryMem0Stub store;
  private MemoryServiceImpl memoryService;
  private Session session;
  private Profile profile;
  private CountingToolInvocationAudit toolAudit;
  private CountingChatPort countingChatPort;

  @BeforeEach
  void setUp() {
    store = new InMemoryMem0Stub();
    memoryService = new MemoryServiceImpl(store);
    session = new Session("flow-test-1", "default", "cli", "test-user");
    profile =
        new Profile(
            "test",
            "test profile",
            new Profile.Identity("agent", "You are a helpful assistant."),
            new Profile.ProviderConfig("mock", "mock-model", null),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            Map.of(),
            10,
            20,
            null);
    toolAudit = new CountingToolInvocationAudit();
  }

  @Test
  @DisplayName("完整 ReAct 两轮：save_memory 恰调一次、记忆写入、会话历史完整")
  void fullReactFlowWithSaveMemory() {
    MemoryTools memoryTools = new MemoryTools(memoryService);
    SimpleToolRegistry registry = new SimpleToolRegistry();
    registry.register(memoryTools.saveMemoryTool());

    ToolExecutor toolExecutor = new ToolExecutor(registry, toolAudit);
    PromptBuilder promptBuilder = new PromptBuilder(new ContextLoader(), memoryService);

    MockFlowChatPort mockChat = new MockFlowChatPort();
    countingChatPort = new CountingChatPort(mockChat);

    ReActLoop loop = new ReActLoop(promptBuilder, countingChatPort, toolExecutor);

    String result = loop.run(session, "记住：我喜欢喝美式咖啡", profile);

    assertNotNull(result);
    assertFalse(result.isEmpty());
    assertEquals("已记住，这是 mock 的确认答复。", result);

    assertEquals(2, countingChatPort.callCount.get(), "ChatPort 应被调用 2 次（两轮 ReAct）");

    assertEquals(1, toolAudit.callCount.get(), "save_memory 应恰好被调一次");
    assertEquals("save_memory", toolAudit.lastToolName);
    assertTrue(toolAudit.lastInputJson.contains("mock 测试记忆"));
    assertTrue(toolAudit.lastSuccess);

    List<InMemoryMem0Stub.Entry> entries = store.allEntries();
    assertEquals(1, entries.size());
    assertEquals(MemoryScope.CORE, entries.get(0).scope());
    assertTrue(entries.get(0).content().contains("mock 测试记忆"));

    List<Message> messages = session.messages();
    assertTrue(messages.size() >= 4, "会话历史应至少有：user + assistant(tool_call) + tool + assistant");
    assertEquals(Message.Role.USER, messages.get(0).role());
    assertEquals("记住：我喜欢喝美式咖啡", messages.get(0).content());
  }

  @Test
  @DisplayName("无工具调用的简单对话：一轮即返回")
  void simpleConversationWithoutToolCalls() {
    SimpleToolRegistry registry = new SimpleToolRegistry();
    ToolExecutor toolExecutor = new ToolExecutor(registry, toolAudit);
    PromptBuilder promptBuilder = new PromptBuilder(new ContextLoader(), memoryService);

    ChatPort textOnlyChatPort =
        (sessionId, prof, prompt) -> new ChatResponse("你好，有什么可以帮你的？", Collections.emptyList());
    countingChatPort = new CountingChatPort(textOnlyChatPort);

    ReActLoop loop = new ReActLoop(promptBuilder, countingChatPort, toolExecutor);

    String result = loop.run(session, "你好", profile);

    assertEquals("你好，有什么可以帮你的？", result);
    assertEquals(1, countingChatPort.callCount.get(), "无工具调用时应只调一次 ChatPort");
    assertEquals(0, toolAudit.callCount.get(), "不应有工具调用");

    List<Message> messages = session.messages();
    assertEquals(2, messages.size());
    assertEquals(Message.Role.USER, messages.get(0).role());
    assertEquals(Message.Role.ASSISTANT, messages.get(1).role());
  }

  static class SimpleToolRegistry implements ToolRegistry {
    private final Map<String, IbiosTool> tools = new HashMap<>();

    @Override
    public IbiosTool lookup(String name) {
      return tools.get(name);
    }

    @Override
    public List<IbiosTool> listAll() {
      return new ArrayList<>(tools.values());
    }

    @Override
    public void register(IbiosTool tool) {
      tools.put(tool.name(), tool);
    }
  }

  static class CountingChatPort implements ChatPort {
    final AtomicInteger callCount = new AtomicInteger(0);
    private final ChatPort delegate;

    CountingChatPort(ChatPort delegate) {
      this.delegate = delegate;
    }

    @Override
    public ChatResponse chat(String sessionId, Profile profile, Prompt prompt) {
      callCount.incrementAndGet();
      return delegate.chat(sessionId, profile, prompt);
    }
  }

  static class MockFlowChatPort implements ChatPort {
    private boolean firstCall = true;

    @Override
    public ChatResponse chat(String sessionId, Profile profile, Prompt prompt) {
      if (firstCall) {
        firstCall = false;
        return new ChatResponse(
            "",
            List.of(
                new ToolCallResponse(
                    "save_memory", "{\"content\":\"mock 测试记忆\",\"scope\":\"core\"}")));
      }
      return new ChatResponse("已记住，这是 mock 的确认答复。", Collections.emptyList());
    }
  }

  static class CountingToolInvocationAudit implements ToolInvocationAudit {
    final AtomicInteger callCount = new AtomicInteger(0);
    String lastToolName;
    String lastInputJson;
    String lastOutput;
    boolean lastSuccess;
    String lastError;

    @Override
    public void recordToolInvocation(
        String sessionId,
        String toolName,
        String inputJson,
        String outputJson,
        boolean success,
        String errorMessage,
        long durationMs) {
      callCount.incrementAndGet();
      this.lastToolName = toolName;
      this.lastInputJson = inputJson;
      this.lastOutput = outputJson;
      this.lastSuccess = success;
      this.lastError = errorMessage;
    }
  }
}
