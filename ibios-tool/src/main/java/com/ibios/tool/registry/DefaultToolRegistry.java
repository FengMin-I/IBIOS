package com.ibios.tool.registry;

import com.ibios.core.port.ToolRegistry;
import com.ibios.core.tool.IbiosTool;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.springframework.stereotype.Component;

@Component
public class DefaultToolRegistry implements ToolRegistry {

  private final ConcurrentMap<String, IbiosTool> tools = new ConcurrentHashMap<>();

  @Override
  public IbiosTool lookup(String name) {
    return tools.get(name);
  }

  @Override
  public List<IbiosTool> listAll() {
    return Collections.unmodifiableList(new ArrayList<>(tools.values()));
  }

  @Override
  public List<IbiosTool> filterForProfile(List<String> toolNames) {
    if (toolNames == null || toolNames.isEmpty()) {
      return listAll();
    }
    List<IbiosTool> filtered = new ArrayList<>();
    for (String name : toolNames) {
      IbiosTool tool = tools.get(name);
      if (tool != null) {
        filtered.add(tool);
      }
    }
    return Collections.unmodifiableList(filtered);
  }

  @Override
  public void register(IbiosTool tool) {
    tools.put(tool.name(), tool);
  }
}
