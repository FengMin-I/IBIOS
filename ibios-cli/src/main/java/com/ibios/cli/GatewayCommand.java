package com.ibios.cli;

import picocli.CommandLine.Command;

@Command(name = "gateway", description = "启动守护进程，同时挂多个 Channel")
public class GatewayCommand implements Runnable {

  @Override
  public void run() {
    System.out.println("TODO: 启动守护进程（后续节实现）");
  }
}
