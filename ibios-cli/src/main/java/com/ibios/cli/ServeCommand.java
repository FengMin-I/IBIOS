package com.ibios.cli;

import picocli.CommandLine.Command;

@Command(name = "serve", description = "启动 Web Service，提供 REST API")
public class ServeCommand implements Runnable {

  @Override
  public void run() {
    System.out.println("TODO: 启动 Web Service（第 26 节实现）");
  }
}
