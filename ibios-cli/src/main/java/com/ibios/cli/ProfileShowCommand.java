package com.ibios.cli;

import com.ibios.core.workspace.WorkspaceRoot;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "show", description = "查看指定 Agent Profile 详情")
public class ProfileShowCommand implements Runnable {

  @Parameters(index = "0", description = "Profile 名称")
  String name;

  @Override
  public void run() {
    Path file = WorkspaceRoot.resolve().profilesDir().resolve(name + ".yaml");
    if (!Files.exists(file)) {
      System.err.println("Profile 不存在: " + name);
      return;
    }
    try {
      System.out.println(Files.readString(file));
    } catch (IOException e) {
      System.err.println("读取 Profile 失败: " + e.getMessage());
    }
  }
}
