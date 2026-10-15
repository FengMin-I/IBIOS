package com.ibios.memory;

import com.ibios.core.memory.LongTermMemoryStore;
import com.ibios.core.memory.MemoryScope;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import org.springframework.data.domain.PageRequest;

public class SqliteMemoryStore implements LongTermMemoryStore {

  private static final int MAX_ARCHIVE_ROWS = 100;

  private final MemoryEntryRepository repository;

  public SqliteMemoryStore(MemoryEntryRepository repository) {
    this.repository = repository;
  }

  @Override
  public void append(String content, MemoryScope scope) {
    MemoryEntry entry = new MemoryEntry(scope.name(), content, LocalDate.now().toString());
    repository.save(entry);
  }

  @Override
  public String load() {
    StringBuilder sb = new StringBuilder();

    List<MemoryEntry> coreEntries = repository.findByScope(MemoryScope.CORE.name());
    if (!coreEntries.isEmpty()) {
      sb.append("## 核心记忆\n");
      for (MemoryEntry entry : coreEntries) {
        sb.append("- [")
            .append(entry.getCreatedAt())
            .append("] ")
            .append(entry.getContent())
            .append("\n");
      }
    }

    List<MemoryEntry> archivalEntries =
        repository.findRecentArchival(PageRequest.of(0, MAX_ARCHIVE_ROWS));
    if (!archivalEntries.isEmpty()) {
      sb.append("\n## 归档记忆\n");
      Collections.reverse(archivalEntries);
      for (MemoryEntry entry : archivalEntries) {
        sb.append("- [")
            .append(entry.getCreatedAt())
            .append("] ")
            .append(entry.getContent())
            .append("\n");
      }
    }

    return sb.toString();
  }

  @Override
  public List<String> recallByKeyword(String keyword) {
    List<MemoryEntry> hits = repository.searchArchival(keyword);
    return hits.stream().map(MemoryEntry::getContent).toList();
  }
}
