package com.ibios.channel.cli;

import com.ibios.core.react.AgentService;
import com.ibios.core.session.Session;
import com.ibios.core.session.SessionManager;
import java.util.Scanner;

public class CliChannel {

  private final SessionManager sessionManager;
  private final AgentService agentService;

  public CliChannel(SessionManager sessionManager, AgentService agentService) {
    this.sessionManager = sessionManager;
    this.agentService = agentService;
  }

  public void start(String profileName) {
    String userId = System.getProperty("user.name", "local");
    Session session = sessionManager.getOrCreate("cli", userId, profileName);
    System.out.println("已连接到 Agent: " + profileName + " (session: " + session.id() + ")");
    System.out.println("输入 /quit 退出");

    Scanner scanner = new Scanner(System.in);
    while (true) {
      System.out.print("> ");
      if (!scanner.hasNextLine()) {
        break;
      }
      String line = scanner.nextLine().trim();
      if ("/quit".equals(line)) {
        System.out.println("再见！");
        break;
      }
      if (line.isEmpty()) {
        continue;
      }
      String reply = agentService.process(session, line);
      System.out.println(reply);
    }
  }
}
