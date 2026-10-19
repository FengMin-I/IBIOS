package com.ibios.memory;

import com.ibios.core.agent.ToolExecutionContext;
import com.ibios.core.memory.LongTermMemoryStore;
import com.ibios.core.memory.MemoryScope;
import com.ibios.core.memory.MemoryService;
import com.ibios.core.session.Session;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class MemoryServiceImpl implements MemoryService {

  private static final Path AGENTS_DIR = Path.of(".ibios", "agents");

  private final LongTermMemoryStore store;

  public MemoryServiceImpl(LongTermMemoryStore store) {
    this.store = store;
  }

  @Override
  public String buildContext(Session session) {
    String agentName = ToolExecutionContext.getAgentName();
    if (agentName == null && session != null) {
      agentName = session.profileName();
    }
    MarkdownMemoryStore agentStore = resolveAgentStore(agentName);
    if (agentStore != null) {
      return Files.exists(agentStore.getMemoryFilePath()) ? agentStore.load() : "";
    }
    return store.load();
  }

  @Override
  public void remember(String content, MemoryScope scope) {
    String agentName = ToolExecutionContext.getAgentName();
    MarkdownMemoryStore agentStore = resolveAgentStore(agentName);
    if (agentStore != null) {
      try {
        Files.createDirectories(agentStore.getMemoryFilePath().getParent());
      } catch (IOException e) {
        throw new IllegalStateException("Failed to create agent memory dir", e);
      }
      agentStore.append(content, scope);
      return;
    }
    store.append(content, scope);
  }

  @Override
  public List<String> recall(String keyword) {
    String agentName = ToolExecutionContext.getAgentName();
    MarkdownMemoryStore agentStore = resolveAgentStore(agentName);
    if (agentStore != null) {
      return agentStore.recallByKeyword(keyword);
    }
    return store.recallByKeyword(keyword);
  }

  // 只有真实存在的 Agent 目录才使用独立记忆；普通 Profile 仍走全局存储
  private MarkdownMemoryStore resolveAgentStore(String agentName) {
    if (agentName == null || !(store instanceof MarkdownMemoryStore)) {
      return null;
    }
    Path agentDir = AGENTS_DIR.resolve(agentName);
    if (!Files.isDirectory(agentDir)) {
      return null;
    }
    return new MarkdownMemoryStore(agentDir.resolve("MEMORY.md"));
  }
}
