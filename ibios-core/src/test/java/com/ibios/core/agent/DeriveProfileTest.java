package com.ibios.core.agent;

import static org.junit.jupiter.api.Assertions.*;

import com.ibios.core.profile.Profile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DeriveProfileTest {

  @TempDir Path tempDir;

  private final AgentLoader loader = new AgentLoader();

  @Test
  @DisplayName("frontmatter 各字段正确映射到 Profile")
  void mapsFrontmatterToProfile() throws IOException {
    Path agentDir = tempDir.resolve("my-agent");
    Files.createDirectories(agentDir);
    Files.writeString(
        agentDir.resolve("AGENT.md"),
        "---\n"
            + "name: my-agent\n"
            + "description: A test agent\n"
            + "identity:\n"
            + "  agent_name: TestBot\n"
            + "  prompt: You are a test bot.\n"
            + "provider:\n"
            + "  name: deepseek\n"
            + "  model: deepseek-chat\n"
            + "  temperature: 0.3\n"
            + "max_iterations: 5\n"
            + "max_history_turns: 10\n"
            + "---\n\n"
            + "# Task\n\nDo something.");

    Profile profile = loader.deriveProfile(agentDir);

    assertEquals("my-agent", profile.name());
    assertEquals("A test agent", profile.description());
    assertEquals("TestBot", profile.identity().agentName());
    assertEquals("You are a test bot.", profile.identity().prompt());
    assertEquals("deepseek", profile.provider().name());
    assertEquals("deepseek-chat", profile.provider().model());
    assertEquals(0.3, profile.provider().temperature());
    assertEquals(5, profile.maxIterations());
    assertEquals(10, profile.maxHistoryTurns());
  }

  @Test
  @DisplayName("agentDir 设为目录绝对路径")
  void setsAgentDir() throws IOException {
    Path agentDir = tempDir.resolve("dir-agent");
    Files.createDirectories(agentDir);
    Files.writeString(
        agentDir.resolve("AGENT.md"),
        "---\nname: dir-agent\nprovider:\n  name: deepseek\n---\nBody");

    Profile profile = loader.deriveProfile(agentDir);

    assertNotNull(profile.agentDir());
    assertEquals(agentDir.toAbsolutePath(), profile.agentDir());
  }

  @Test
  @DisplayName("schedules 原样带入 Profile")
  void carriesSchedulesIntoProfile() throws IOException {
    Path agentDir = tempDir.resolve("sched-agent");
    Files.createDirectories(agentDir);
    Files.writeString(
        agentDir.resolve("AGENT.md"),
        "---\n"
            + "name: sched-agent\n"
            + "provider:\n"
            + "  name: deepseek\n"
            + "schedules:\n"
            + "  - {id: daily, cron: '0 0 9 * * *', zone: Asia/Shanghai, message: hello}\n"
            + "---\n\nBody");

    Profile profile = loader.deriveProfile(agentDir);

    assertEquals(1, profile.schedules().size());
    Profile.ScheduleConfig sc = profile.schedules().get(0);
    assertEquals("daily", sc.id());
    assertEquals("0 0 9 * * *", sc.cron());
    assertEquals("Asia/Shanghai", sc.zone());
    assertEquals("hello", sc.message());
  }

  @Test
  @DisplayName("name 取 frontmatter 的 name 字段")
  void nameFromFrontmatter() throws IOException {
    Path agentDir = tempDir.resolve("named-agent");
    Files.createDirectories(agentDir);
    Files.writeString(
        agentDir.resolve("AGENT.md"),
        "---\nname: custom-name\nprovider:\n  name: deepseek\n---\nBody");

    Profile profile = loader.deriveProfile(agentDir);

    assertEquals("custom-name", profile.name());
  }
}
