package com.ibios.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "create", description = "创建一个新的 Agent Profile")
public class ProfileCreateCommand implements Runnable {

  @Parameters(index = "0", description = "Profile 名称")
  String name;

  @Override
  public void run() {
    Path profilesDir = Path.of(".ibios/profiles");
    try {
      Files.createDirectories(profilesDir);
      Path file = profilesDir.resolve(name + ".yaml");
      if (Files.exists(file)) {
        System.err.println("Profile 已存在: " + name);
        return;
      }
      String content =
          "name: "
              + name
              + "\n"
              + "description: \"\"\n"
              + "identity:\n"
              + "  agent_name: "
              + name
              + "\n"
              + "  prompt: \"\"\n"
              + "provider:\n"
              + "  name: dashscope\n"
              + "  model: qwen-plus\n";
      Files.writeString(file, content);
      System.out.println("Profile 已创建: " + file);
    } catch (IOException e) {
      System.err.println("创建 Profile 失败: " + e.getMessage());
    }
  }
}
