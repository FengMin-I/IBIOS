package com.ibios.cli;

import picocli.CommandLine.Command;

@Command(
    name = "profile",
    description = "管理 Agent Profile",
    subcommands = {
      ProfileListCommand.class,
      ProfileCreateCommand.class,
      ProfileShowCommand.class,
      ProfileDeleteCommand.class
    })
public class ProfileCommand implements Runnable {

  @Override
  public void run() {
    picocli.CommandLine.usage(this, System.out);
  }
}
