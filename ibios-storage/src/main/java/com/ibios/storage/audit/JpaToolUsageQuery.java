package com.ibios.storage.audit;

import com.ibios.core.audit.ToolUsageQuery;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class JpaToolUsageQuery implements ToolUsageQuery {

  private final ToolInvocationRepository repository;

  public JpaToolUsageQuery(ToolInvocationRepository repository) {
    this.repository = repository;
  }

  @Override
  public Map<String, List<String>> findUsedToolNamesByProfile() {
    Map<String, List<String>> grouped = new LinkedHashMap<>();
    for (Object[] row : repository.findProfileToolPairs()) {
      String profileName = (String) row[0];
      String toolName = (String) row[1];
      grouped.computeIfAbsent(profileName, key -> new ArrayList<>()).add(toolName);
    }
    for (Map.Entry<String, List<String>> entry : grouped.entrySet()) {
      entry.setValue(Collections.unmodifiableList(entry.getValue()));
    }
    return Collections.unmodifiableMap(grouped);
  }
}
