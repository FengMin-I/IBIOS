package com.ibios.core.memory;

import com.ibios.core.session.Session;
import java.util.List;

public interface MemoryService {

  String buildContext(Session session);

  void remember(String content, MemoryScope scope);

  List<String> recall(String keyword);
}
