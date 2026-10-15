package com.ibios.memory;

import com.ibios.core.memory.LongTermMemoryStore;
import com.ibios.core.memory.MemoryScope;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InMemoryMem0Stub implements LongTermMemoryStore {

  private static final int MAX_ARCHIVE_ROWS = 100;

  private final List<Entry> entries = new ArrayList<>();

  @Override
  public void append(String content, MemoryScope scope) {
    entries.add(new Entry(scope, content));
  }

  @Override
  public String load() {
    StringBuilder sb = new StringBuilder();

    List<Entry> core = entries.stream().filter(e -> e.scope == MemoryScope.CORE).toList();
    if (!core.isEmpty()) {
      sb.append("## 核心记忆\n");
      for (Entry e : core) {
        sb.append("- ").append(e.content).append("\n");
      }
    }

    List<Entry> archival = entries.stream().filter(e -> e.scope == MemoryScope.ARCHIVAL).toList();
    int start = Math.max(0, archival.size() - MAX_ARCHIVE_ROWS);
    List<Entry> truncated = archival.subList(start, archival.size());
    if (!truncated.isEmpty()) {
      sb.append("\n## 归档记忆\n");
      for (Entry e : truncated) {
        sb.append("- ").append(e.content).append("\n");
      }
    }

    return sb.toString();
  }

  @Override
  public List<String> recallByKeyword(String keyword) {
    return entries.stream()
        .filter(e -> e.scope == MemoryScope.ARCHIVAL)
        .filter(e -> e.content.contains(keyword))
        .map(e -> e.content)
        .toList();
  }

  public void clear() {
    entries.clear();
  }

  public List<Entry> allEntries() {
    return Collections.unmodifiableList(entries);
  }

  record Entry(MemoryScope scope, String content) {}
}
