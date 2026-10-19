package com.ibios.core.port;

import com.ibios.core.tool.IbiosTool;
import java.util.List;

public interface ToolRegistry {

  IbiosTool lookup(String name);

  List<IbiosTool> listAll();

  void register(IbiosTool tool);
}
