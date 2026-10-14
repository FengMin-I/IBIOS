package com.ibios.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import picocli.CommandLine.Command;

@Command(name = "list", description = "列出所有 Agent Profile")
public class ProfileListCommand implements Runnable {

  @Override
  public void run() {
    Path profilesDir = Path.of(".ibios/profiles");
    if (!Files.isDirectory(profilesDir)) {
      System.out.println("暂无 Profile，请先运行 ibios init");
      return;
    }
    try (Stream<Path> files = Files.list(profilesDir)) {
      files
          .filter(p -> p.toString().endsWith(".yaml") || p.toString().endsWith(".yml"))
          .forEach(p -> System.out.println(p.getFileName()));
    } catch (IOException e) {
      System.err.println("读取 Profile 目录失败: " + e.getMessage());
    }
  }
}
