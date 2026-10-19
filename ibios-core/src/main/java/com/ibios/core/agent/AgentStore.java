package com.ibios.core.agent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class AgentStore {

  private static final Logger LOG = Logger.getLogger(AgentStore.class.getName());

  private final Path agentsDir;
  private final Path archiveDir;

  public AgentStore(Path agentsDir, Path archiveDir) {
    this.agentsDir = agentsDir;
    this.archiveDir = archiveDir;
  }

  public Path write(String name, String agentMarkdown) {
    Path agentDir = agentsDir.resolve(name);
    try {
      Files.createDirectories(agentDir);
      Path agentMd = agentDir.resolve("AGENT.md");
      Files.writeString(agentMd, agentMarkdown);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to write agent directory: " + agentDir, e);
    }
    return agentDir;
  }

  public Path writeAll(String name, Map<String, String> files) {
    Path agentDir = agentsDir.resolve(name);
    try {
      if (Files.exists(agentDir)) {
        deleteDirectory(agentDir);
      }
      Files.createDirectories(agentDir);
      for (Map.Entry<String, String> entry : files.entrySet()) {
        Path filePath = agentDir.resolve(entry.getKey());
        Files.createDirectories(filePath.getParent());
        Files.writeString(filePath, entry.getValue());
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to write agent files: " + agentDir, e);
    }
    return agentDir;
  }

  public void archive(String name) {
    Path agentDir = agentsDir.resolve(name);
    if (!Files.isDirectory(agentDir)) {
      return;
    }
    Path archivePath = archiveDir.resolve(name);
    try {
      Files.createDirectories(archiveDir);
      if (Files.exists(archivePath)) {
        deleteDirectory(archivePath);
      }
      Files.move(agentDir, archivePath, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to archive agent: " + name, e);
    }
  }

  public void delete(Path agentDir) {
    if (!Files.exists(agentDir)) {
      return;
    }
    try {
      deleteDirectory(agentDir);
    } catch (IOException e) {
      LOG.log(Level.WARNING, "Failed to delete agent directory: " + agentDir, e);
    }
  }

  public Path agentsDir() {
    return agentsDir;
  }

  public Path archiveDir() {
    return archiveDir;
  }

  private void deleteDirectory(Path dir) throws IOException {
    if (!Files.exists(dir)) {
      return;
    }
    try (Stream<Path> walk = Files.walk(dir)) {
      walk.sorted(java.util.Comparator.reverseOrder())
          .forEach(
              path -> {
                try {
                  Files.deleteIfExists(path);
                } catch (IOException e) {
                  LOG.log(Level.WARNING, "Failed to delete: " + path, e);
                }
              });
    }
  }
}
