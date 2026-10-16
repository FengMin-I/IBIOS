package com.ibios.core.react;

import static org.junit.jupiter.api.Assertions.*;

import com.ibios.core.profile.Profile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProgressiveDisclosureTest {

  @TempDir Path tempDir;

  private final ContextLoader contextLoader = new ContextLoader();

  @Test
  @DisplayName("Agent 正文进 system prompt（agentDir 非空时 loadAgentBody 返回正文）")
  void agentBodyLoadedWhenAgentDirSet() throws IOException {
    Path agentDir = tempDir.resolve("my-agent");
    Files.createDirectories(agentDir);
    Files.writeString(
        agentDir.resolve("AGENT.md"),
        "---\nname: my-agent\nprovider:\n  name: deepseek\n---\n\n# Task\n\nDo something.");

    Profile profile = createProfileWithAgentDir(agentDir);

    String body = contextLoader.loadAgentBody(profile);

    assertTrue(body.contains("# Task"));
    assertTrue(body.contains("Do something."));
    assertFalse(body.contains("name: my-agent"));
  }

  @Test
  @DisplayName("agentDir 为 null 时返回空字符串")
  void emptyBodyWhenAgentDirNull() {
    Profile profile = createProfileWithoutAgentDir();

    String body = contextLoader.loadAgentBody(profile);

    assertTrue(body.isEmpty());
  }

  @Test
  @DisplayName("参考/脚本内容不预载（loadAgentBody 只返回正文，不含 skills/ 和 scripts/ 内容）")
  void referenceAndScriptsNotPreloaded() throws IOException {
    Path agentDir = tempDir.resolve("res-agent");
    Files.createDirectories(agentDir.resolve("scripts"));
    Files.createDirectories(agentDir.resolve("skills"));
    Files.writeString(agentDir.resolve("REFERENCE.md"), "secret reference content");
    Files.writeString(agentDir.resolve("scripts/data.py"), "print('secret')");
    Files.writeString(
        agentDir.resolve("AGENT.md"),
        "---\nname: res-agent\nprovider:\n  name: deepseek\n---\n\nJust the task body.");

    Profile profile = createProfileWithAgentDir(agentDir);

    String body = contextLoader.loadAgentBody(profile);

    assertTrue(body.contains("Just the task body."));
    assertFalse(body.contains("secret reference content"));
    assertFalse(body.contains("print('secret')"));
  }

  private Profile createProfileWithAgentDir(Path agentDir) {
    return new Profile(
        "test",
        "test",
        new Profile.Identity("agent", "prompt"),
        new Profile.ProviderConfig("dashscope", "qwen-plus", null),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        Map.of(),
        10,
        20,
        agentDir.toAbsolutePath());
  }

  private Profile createProfileWithoutAgentDir() {
    return new Profile(
        "test",
        "test",
        new Profile.Identity("agent", "prompt"),
        new Profile.ProviderConfig("dashscope", "qwen-plus", null),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        Map.of(),
        10,
        20,
        null);
  }
}
