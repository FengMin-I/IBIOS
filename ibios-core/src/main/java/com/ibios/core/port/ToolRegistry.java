package com.ibios.core.port;

import com.ibios.core.tool.IbiosTool;
import java.util.List;

public interface ToolRegistry {

  IbiosTool lookup(String name);

  List<IbiosTool> listAll();

  List<IbiosTool> filterForProfile(List<String> toolNames);

  void register(IbiosTool tool);
}
