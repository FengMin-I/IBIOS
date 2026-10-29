package com.ibios.tool.sandbox;

import com.ibios.core.sandbox.SandboxWhitelistStore;
import com.ibios.core.sandbox.WhitelistEntryView;
import com.ibios.core.sandbox.WhitelistType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/** 单测用的内存版白名单 store，行为对齐 JPA 实现：唯一约束、按 type 排序。 */
public class InMemorySandboxWhitelistStore implements SandboxWhitelistStore {

  private final List<WhitelistEntryView> entries = new ArrayList<>();
  private final AtomicLong sequence = new AtomicLong();

  @Override
  public synchronized List<WhitelistEntryView> listAll() {
    return entries.stream()
        .sorted(
            Comparator.comparing(WhitelistEntryView::type).thenComparing(WhitelistEntryView::id))
        .toList();
  }

  @Override
  public synchronized WhitelistEntryView findById(Long id) {
    return entries.stream().filter(entry -> entry.id().equals(id)).findFirst().orElse(null);
  }

  @Override
  public synchronized boolean exists(WhitelistType type, String value) {
    return entries.stream().anyMatch(entry -> entry.type() == type && entry.value().equals(value));
  }

  @Override
  public synchronized WhitelistEntryView create(WhitelistType type, String value) {
    if (exists(type, value)) {
      throw new IllegalArgumentException("Whitelist entry already exists: " + value);
    }
    WhitelistEntryView created = new WhitelistEntryView(sequence.incrementAndGet(), type, value);
    entries.add(created);
    return created;
  }

  @Override
  public synchronized boolean delete(Long id) {
    return entries.removeIf(entry -> entry.id().equals(id));
  }
}
