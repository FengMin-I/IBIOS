package com.ibios.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "delete", description = "删除指定 Agent Profile")
public class ProfileDeleteCommand implements Runnable {

  @Parameters(index = "0", description = "Profile 名称")
  String name;

  @Override
  public void run() {
    Path file = Path.of(".ibios/profiles/" + name + ".yaml");
    if (!Files.exists(file)) {
      System.err.println("Profile 不存在: " + name);
      return;
    }
    try {
      Files.delete(file);
      System.out.println("Profile 已删除: " + name);
    } catch (IOException e) {
      System.err.println("删除 Profile 失败: " + e.getMessage());
    }
  }
}
