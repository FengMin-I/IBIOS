package com.ibios.memory;

import com.ibios.core.memory.LongTermMemoryStore;
import com.ibios.core.memory.MemoryScope;
import com.ibios.core.memory.MemoryService;
import com.ibios.core.session.Session;
import java.util.List;

public class MemoryServiceImpl implements MemoryService {

  private final LongTermMemoryStore store;

  public MemoryServiceImpl(LongTermMemoryStore store) {
    this.store = store;
  }

  @Override
  public String buildContext(Session session) {
    return store.load();
  }

  @Override
  public void remember(String content, MemoryScope scope) {
    store.append(content, scope);
  }

  @Override
  public List<String> recall(String keyword) {
    return store.recallByKeyword(keyword);
  }
}
