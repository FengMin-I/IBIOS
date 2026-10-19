package com.ibios.core.agent;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AgentStoreTest {

  @TempDir Path tempDir;

  private Path agentsDir;
  private Path archiveDir;
  private AgentStore store;

  @BeforeEach
  void setUp() {
    agentsDir = tempDir.resolve("agents");
    archiveDir = tempDir.resolve("archive");
    store = new AgentStore(agentsDir, archiveDir);
  }

  @Test
  @DisplayName("write 在 agents 目录下创建 AGENT.md 文件")
  void writeCreatesAgentMdInAgentsDir() throws IOException {
    String name = "my-agent";
    String content = "# My Agent\nThis is a test agent.";

    Path result = store.write(name, content);

    assertTrue(Files.exists(result), "Agent directory should be created");
    assertTrue(Files.isDirectory(result), "Result should be a directory");

    Path agentMd = result.resolve("AGENT.md");
    assertTrue(Files.exists(agentMd), "AGENT.md should exist");

    String actual = Files.readString(agentMd);
    assertTrue(actual.equals(content), "AGENT.md content should match");
  }

  @Test
  @DisplayName("writeAll 创建多个文件")
  void writeAllCreatesMultipleFiles() throws IOException {
    String name = "multi-agent";
    Map<String, String> files = new LinkedHashMap<>();
    files.put("AGENT.md", "# Agent");
    files.put("skills/tool.md", "# Skill");
    files.put("scripts/run.py", "print('hello')");

    Path result = store.writeAll(name, files);

    assertTrue(Files.isDirectory(result), "Agent directory should be created");

    for (Map.Entry<String, String> entry : files.entrySet()) {
      Path filePath = result.resolve(entry.getKey());
      assertTrue(Files.exists(filePath), "File should exist: " + entry.getKey());
      String actual = Files.readString(filePath);
      assertTrue(actual.equals(entry.getValue()), "Content should match for: " + entry.getKey());
    }
  }

  @Test
  @DisplayName("archive 将目录从 agents/ 移动到 archive/")
  void archiveMovesDirFromAgentsToArchive() throws IOException {
    String name = "to-archive";
    store.write(name, "# Archive Me");

    Path agentDir = agentsDir.resolve(name);
    assertTrue(Files.exists(agentDir), "Agent dir should exist before archive");

    store.archive(name);

    assertFalse(Files.exists(agentDir), "Agent dir should be moved");

    Path archivePath = archiveDir.resolve(name);
    assertTrue(Files.exists(archivePath), "Archive dir should exist");
    assertTrue(Files.isDirectory(archivePath), "Archive should be a directory");

    Path archivedMd = archivePath.resolve("AGENT.md");
    assertTrue(Files.exists(archivedMd), "AGENT.md should be in archive");
  }

  @Test
  @DisplayName("delete 删除整个目录")
  void deleteRemovesDirectory() throws IOException {
    String name = "to-delete";
    Path agentDir = store.write(name, "# Delete Me");

    assertTrue(Files.exists(agentDir), "Agent dir should exist before delete");

    store.delete(agentDir);

    assertFalse(Files.exists(agentDir), "Agent dir should be deleted");
  }

  @Test
  @DisplayName("delete 不存在的目录不会抛异常")
  void deleteNonExistentDirIsSafe() {
    Path nonExistent = agentsDir.resolve("does-not-exist");

    assertFalse(Files.exists(nonExistent), "Path should not exist");

    store.delete(nonExistent);

    assertFalse(Files.exists(nonExistent), "Path should still not exist");
  }
}
