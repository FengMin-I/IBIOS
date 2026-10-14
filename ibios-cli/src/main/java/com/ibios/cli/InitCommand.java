package com.ibios.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import picocli.CommandLine.Command;

@Command(name = "init", description = "初始化一个 IBIOS 工程")
public class InitCommand implements Runnable {

  @Override
  public void run() {
    Path workDir = Path.of(".ibios");
    try {
      Files.createDirectories(workDir.resolve("agents"));
      Files.createDirectories(workDir.resolve("skills"));
      Files.createDirectories(workDir.resolve("profiles"));
      Path mcpConfig = workDir.resolve("mcp_servers.yaml");
      if (!Files.exists(mcpConfig)) {
        Files.writeString(mcpConfig, "# MCP server 配置\nservers: []\n");
      }
      System.out.println("IBIOS 工程初始化完成: " + workDir.toAbsolutePath());
    } catch (IOException e) {
      System.err.println("初始化失败: " + e.getMessage());
    }
  }
}
