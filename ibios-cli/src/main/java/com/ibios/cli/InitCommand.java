package com.ibios.cli;

import com.ibios.core.workspace.WorkspaceRoot;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import picocli.CommandLine.Command;

@Command(name = "init", description = "初始化一个 IBIOS 工程")
public class InitCommand implements Runnable {

  @Override
  public void run() {
    WorkspaceRoot workspaceRoot = WorkspaceRoot.resolve();
    Path workDir = workspaceRoot.path();
    try {
      Files.createDirectories(workspaceRoot.agentsDir());
      Files.createDirectories(workspaceRoot.skillsDir());
      Files.createDirectories(workspaceRoot.profilesDir());
      Path mcpConfig = workspaceRoot.mcpConfigFile();
      if (!Files.exists(mcpConfig)) {
        Files.writeString(mcpConfig, "# MCP server 配置\nservers: []\n");
      }
      System.out.println("IBIOS 工程初始化完成: " + workDir.toAbsolutePath());
    } catch (IOException e) {
      System.err.println("初始化失败: " + e.getMessage());
    }
  }
}
