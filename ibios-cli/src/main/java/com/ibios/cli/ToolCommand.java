package com.ibios.cli;

import picocli.CommandLine.Command;

@Command(name = "tool", description = "管理工具", subcommands = ToolListCommand.class)
public class ToolCommand implements Runnable {

  @Override
  public void run() {
    picocli.CommandLine.usage(this, System.out);
  }
}
