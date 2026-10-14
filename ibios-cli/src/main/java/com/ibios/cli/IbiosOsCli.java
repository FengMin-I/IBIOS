package com.ibios.cli;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
    name = "ibios",
    description = "IBIOS Agent OS 命令行入口",
    mixinStandardHelpOptions = true,
    version = "0.0.1-SNAPSHOT",
    subcommands = {
      InitCommand.class,
      StatusCommand.class,
      ChatCommand.class,
      ServeCommand.class,
      GatewayCommand.class,
      ProfileCommand.class,
      ProviderCommand.class,
      ToolCommand.class,
      SessionCommand.class
    })
public class IbiosOsCli implements Runnable {

  @Override
  public void run() {
    CommandLine.usage(this, System.out);
  }

  public static void main(String[] args) {
    int exitCode = new CommandLine(new IbiosOsCli()).execute(args);
    System.exit(exitCode);
  }
}
