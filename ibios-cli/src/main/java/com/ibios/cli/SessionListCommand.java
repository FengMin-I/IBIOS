package com.ibios.cli;

import picocli.CommandLine.Command;

@Command(name = "list", description = "列出会话")
public class SessionListCommand implements Runnable {

  @Override
  public void run() {
    System.out.println("会话列表:");
    System.out.println("  (需要启动 Spring 上下文才能查询数据库)");
    System.out.println("提示: 运行 ibios chat 启动完整环境后查看会话");
  }
}
