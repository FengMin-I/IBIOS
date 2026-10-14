package com.ibios.cli;

import picocli.CommandLine.Command;

@Command(name = "list", description = "列出已注册的工具")
public class ToolListCommand implements Runnable {

  @Override
  public void run() {
    System.out.println("已注册的工具:");
    System.out.println("  (需要启动 Spring 上下文才能获取完整列表)");
    System.out.println("提示: 运行 ibios chat 启动完整环境后查看可用工具");
  }
}
