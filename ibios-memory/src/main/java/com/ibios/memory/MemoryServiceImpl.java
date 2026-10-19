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
    if (store instanceof MarkdownMemoryStore && agentName != null) {
      Path agentMemoryPath = Path.of(".ibios/agents/" + agentName + "/MEMORY.md");
      if (Files.exists(agentMemoryPath)) {
        MarkdownMemoryStore agentStore = new MarkdownMemoryStore(agentMemoryPath);
        return agentStore.load();
      }
    }
    return store.load();
  }

  @Override
  public void remember(String content, MemoryScope scope) {
    String agentName = ToolExecutionContext.getAgentName();
    if (agentName != null && store instanceof MarkdownMemoryStore) {
      Path agentMemoryPath = Path.of(".ibios/agents/" + agentName + "/MEMORY.md");
      try {
        Files.createDirectories(agentMemoryPath.getParent());
      } catch (IOException e) {
        throw new IllegalStateException("Failed to create agent memory dir", e);
      }
      new MarkdownMemoryStore(agentMemoryPath).append(content, scope);
      return;
    }
    store.append(content, scope);
  }

  @Override
  public List<String> recall(String keyword) {
    String agentName = ToolExecutionContext.getAgentName();
    if (agentName != null && store instanceof MarkdownMemoryStore) {
      Path agentMemoryPath = Path.of(".ibios/agents/" + agentName + "/MEMORY.md");
      if (Files.exists(agentMemoryPath)) {
        return new MarkdownMemoryStore(agentMemoryPath).recallByKeyword(keyword);
      }
    }
    return store.recallByKeyword(keyword);
  }
}
