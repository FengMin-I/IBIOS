package com.ibios.cli;

import picocli.CommandLine.Command;

@Command(
    name = "provider",
    description = "管理 LLM Provider",
    subcommands = ProviderListCommand.class)
public class ProviderCommand implements Runnable {

  @Override
  public void run() {
    picocli.CommandLine.usage(this, System.out);
  }
}
