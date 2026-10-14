package com.ibios.cli;

import picocli.CommandLine.Command;

@Command(name = "status", description = "查看当前状态")
public class StatusCommand implements Runnable {

  @Override
  public void run() {
    System.out.println("IBIOS Agent OS v0.0.1-SNAPSHOT");
    System.out.println("工作目录: .ibios/");
    System.out.println("状态: 就绪");
  }
}
