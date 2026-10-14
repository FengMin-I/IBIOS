package com.ibios.cli;

import com.ibios.channel.cli.CliChannel;
import com.ibios.core.react.AgentService;
import com.ibios.core.session.SessionManager;
import org.springframework.context.ConfigurableApplicationContext;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "chat", description = "在终端里和 Agent 交互式对话")
public class ChatCommand implements Runnable {

  @Option(names = "--profile", description = "指定 Agent Profile 名称", defaultValue = "default")
  String profileName;

  @Override
  public void run() {
    ConfigurableApplicationContext ctx = IbiosSpringApp.start(new String[] {});
    try {
      SessionManager sessionManager = ctx.getBean(SessionManager.class);
      AgentService agentService = ctx.getBean(AgentService.class);
      CliChannel cliChannel = new CliChannel(sessionManager, agentService);
      cliChannel.start(profileName);
    } finally {
      ctx.close();
    }
  }
}
