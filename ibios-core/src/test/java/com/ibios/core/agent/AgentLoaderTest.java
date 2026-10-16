package com.ibios.core.agent;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AgentLoaderTest {

  @TempDir Path tempDir;

  private final AgentLoader loader = new AgentLoader();

  @Test
  @DisplayName("正确拆出 frontmatter 与正文")
  void splitsFrontmatterAndBody() throws IOException {
    Path agentDir = tempDir.resolve("test-agent");
    Files.createDirectories(agentDir);
    Files.writeString(
        agentDir.resolve("AGENT.md"),
        "---\nname: test-agent\nprovider:\n  name: deepseek\n---\n\n# Task\n\nDo something.");

    AgentDefinition def = loader.load(agentDir);

    assertEquals("test-agent", def.name());
    assertEquals("test-agent", def.frontmatter().get("name"));
    assertTrue(def.body().contains("# Task"));
    assertTrue(def.body().contains("Do something."));
    assertFalse(def.body().contains("name: test-agent"));
  }

  @Test
  @DisplayName("认出 scripts/ skills/ REFERENCE.md 资源路径")
  void recognizesResourcePaths() throws IOException {
    Path agentDir = tempDir.resolve("res-agent");
    Files.createDirectories(agentDir.resolve("scripts"));
    Files.createDirectories(agentDir.resolve("skills"));
    Files.writeString(agentDir.resolve("REFERENCE.md"), "reference content");
    Files.writeString(
        agentDir.resolve("AGENT.md"),
        "---\nname: res-agent\nprovider:\n  name: deepseek\n---\nBody");

    AgentDefinition def = loader.load(agentDir);

    assertTrue(Files.isDirectory(def.scriptsDir()));
    assertTrue(Files.isDirectory(def.skillsDir()));
    assertTrue(Files.isRegularFile(def.referenceFile()));
  }

  @Test
  @DisplayName("AGENT.md 不存在抛 IllegalArgumentException")
  void missingAgentMdThrows() {
    Path agentDir = tempDir.resolve("empty-agent");

    assertThrows(IllegalArgumentException.class, () -> loader.load(agentDir));
  }

  @Test
  @DisplayName("缺 name 字段抛 IllegalArgumentException")
  void missingNameThrows() throws IOException {
    Path agentDir = tempDir.resolve("no-name-agent");
    Files.createDirectories(agentDir);
    Files.writeString(agentDir.resolve("AGENT.md"), "---\nprovider:\n  name: deepseek\n---\nBody");

    assertThrows(IllegalArgumentException.class, () -> loader.load(agentDir));
  }

  @Test
  @DisplayName("缺 provider 字段抛 IllegalArgumentException")
  void missingProviderThrows() throws IOException {
    Path agentDir = tempDir.resolve("no-provider-agent");
    Files.createDirectories(agentDir);
    Files.writeString(agentDir.resolve("AGENT.md"), "---\nname: no-provider\n---\nBody");

    assertThrows(IllegalArgumentException.class, () -> loader.load(agentDir));
  }

  @Test
  @DisplayName("资源目录不存在时路径为 null")
  void missingResourceDirsAreNull() throws IOException {
    Path agentDir = tempDir.resolve("bare-agent");
    Files.createDirectories(agentDir);
    Files.writeString(
        agentDir.resolve("AGENT.md"), "---\nname: bare\nprovider:\n  name: deepseek\n---\nBody");

    AgentDefinition def = loader.load(agentDir);

    assertNull(def.scriptsDir());
    assertNull(def.skillsDir());
    assertNull(def.referenceFile());
  }
}
