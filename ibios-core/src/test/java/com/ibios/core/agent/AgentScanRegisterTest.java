package com.ibios.core.agent;

import static org.junit.jupiter.api.Assertions.*;

import com.ibios.core.profile.Profile;
import com.ibios.core.profile.ProfileRegistry;
import com.ibios.core.react.AgentScheduler;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

class AgentScanRegisterTest {

  @TempDir Path tempDir;

  private final AgentLoader loader = new AgentLoader();

  @Test
  @DisplayName("N 个 Agent 目录 → ProfileRegistry 出现 N 个")
  void scansAndRegistersAllAgents() throws IOException {
    Path agentsDir = tempDir.resolve("agents");
    Files.createDirectories(agentsDir);
    createAgentDir(agentsDir, "agent-a", "deepseek");
    createAgentDir(agentsDir, "agent-b", "dashscope");
    createAgentDir(agentsDir, "agent-c", "deepseek");

    ProfileRegistry registry = new ProfileRegistry();
    AgentScheduler scheduler = Mockito.mock(AgentScheduler.class);

    loader.scanAndRegister(
        agentsDir, registry, scheduler, Set.of("deepseek", "dashscope"), Set.of());

    assertEquals(3, registry.getAll().size());
    assertNotNull(registry.get("agent-a"));
    assertNotNull(registry.get("agent-b"));
    assertNotNull(registry.get("agent-c"));
  }

  @Test
  @DisplayName("带 schedules 的 Profile 调了 AgentScheduler.registerProfile")
  void schedulesRegisteredWithScheduler() throws IOException {
    Path agentsDir = tempDir.resolve("agents");
    Files.createDirectories(agentsDir);
    Path agentDir = agentsDir.resolve("sched-agent");
    Files.createDirectories(agentDir);
    Files.writeString(
        agentDir.resolve("AGENT.md"),
        "---\nname: sched-agent\nprovider:\n  name: deepseek\nschedules:\n"
            + "  - {id: daily, cron: '0 0 9 * * *', zone: Asia/Shanghai, message: hello}\n"
            + "---\nBody");

    ProfileRegistry registry = new ProfileRegistry();
    AgentScheduler scheduler = Mockito.mock(AgentScheduler.class);

    loader.scanAndRegister(agentsDir, registry, scheduler, Set.of("deepseek"), Set.of());

    Mockito.verify(scheduler).registerProfile(Mockito.any(Profile.class));
  }

  @Test
  @DisplayName("provider 不在 validProviderNames 中 → 报错不注册")
  void invalidProviderSkipped() throws IOException {
    Path agentsDir = tempDir.resolve("agents");
    Files.createDirectories(agentsDir);
    createAgentDir(agentsDir, "bad-agent", "unknown-provider");

    ProfileRegistry registry = new ProfileRegistry();

    loader.scanAndRegister(agentsDir, registry, null, Set.of("deepseek"), Set.of());

    assertNull(registry.get("bad-agent"));
  }

  @Test
  @DisplayName("tools 引用未注册工具 → 告警但不阻断")
  void unregisteredToolWarnsButContinues() throws IOException {
    Path agentsDir = tempDir.resolve("agents");
    Files.createDirectories(agentsDir);
    Path agentDir = agentsDir.resolve("tool-agent");
    Files.createDirectories(agentDir);
    Files.writeString(
        agentDir.resolve("AGENT.md"),
        "---\nname: tool-agent\nprovider:\n  name: deepseek\ntools: [read_file, nonexistent_tool]\n"
            + "---\nBody");

    ProfileRegistry registry = new ProfileRegistry();

    loader.scanAndRegister(agentsDir, registry, null, Set.of("deepseek"), Set.of("read_file"));

    assertNotNull(registry.get("tool-agent"));
  }

  @Test
  @DisplayName("agentsDir 不存在 → 不报错")
  void missingAgentsDirDoesNotThrow() {
    Path agentsDir = tempDir.resolve("nonexistent");
    ProfileRegistry registry = new ProfileRegistry();

    assertDoesNotThrow(() -> loader.scanAndRegister(agentsDir, registry, null, Set.of(), Set.of()));
    assertTrue(registry.getAll().isEmpty());
  }

  private void createAgentDir(Path parent, String name, String provider) throws IOException {
    Path dir = parent.resolve(name);
    Files.createDirectories(dir);
    Files.writeString(
        dir.resolve("AGENT.md"),
        "---\nname: " + name + "\nprovider:\n  name: " + provider + "\n---\nBody for " + name);
  }
}
