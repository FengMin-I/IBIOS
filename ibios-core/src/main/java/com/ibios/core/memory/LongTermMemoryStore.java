package com.ibios.core.memory;

import java.util.List;

public interface LongTermMemoryStore {

  void append(String content, MemoryScope scope);

  String load();

  List<String> recallByKeyword(String keyword);
}
