package com.ibios.cli;

import picocli.CommandLine.Command;

@Command(name = "session", description = "管理会话", subcommands = SessionListCommand.class)
public class SessionCommand implements Runnable {

  @Override
  public void run() {
    picocli.CommandLine.usage(this, System.out);
  }
}
