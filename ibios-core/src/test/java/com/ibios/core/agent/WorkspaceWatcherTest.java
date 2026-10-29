package com.ibios.core.agent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ibios.core.profile.Profile;
import com.ibios.core.profile.ProfileRegistry;
import com.ibios.core.react.AgentScheduler;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class WorkspaceWatcherTest {

  @Mock private AgentLoader agentLoader;
  @Mock private ProfileRegistry profileRegistry;
  @Mock private AgentScheduler agentScheduler;

  @TempDir Path tempDir;

  private WorkspaceWatcher watcher;
  private Path agentsDir;

  private static final String VALID_AGENT_MD =
      "---\nname: test-agent\nprovider:\n  name: dashscope\n  model: qwen-plus\n---\nTest body";

  @BeforeEach
  void setUp() {
    agentsDir = tempDir.resolve("agents");
    try {
      Files.createDirectories(agentsDir);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    watcher =
        new WorkspaceWatcher(
            agentsDir, agentLoader, profileRegistry, agentScheduler, Set.of("dashscope")::contains);
  }

  private Profile buildProfile(String name) {
    return new Profile(
        name,
        "desc",
        new Profile.Identity("agent", "prompt"),
        new Profile.ProviderConfig("dashscope", "qwen-plus", null),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        Map.of(),
        10,
        20,
        agentsDir.resolve(name));
  }

  @Test
  @DisplayName("fullScan registers all agent directories")
  void fullScan_registersAllAgentDirs() throws Exception {
    Path dir1 = agentsDir.resolve("agent-1");
    Files.createDirectories(dir1);
    Files.writeString(dir1.resolve("AGENT.md"), VALID_AGENT_MD);

    Path dir2 = agentsDir.resolve("agent-2");
    Files.createDirectories(dir2);
    Files.writeString(dir2.resolve("AGENT.md"), VALID_AGENT_MD);

    Profile p1 = buildProfile("agent-1");
    Profile p2 = buildProfile("agent-2");
    when(agentLoader.deriveProfile(dir1)).thenReturn(p1);
    when(agentLoader.deriveProfile(dir2)).thenReturn(p2);

    watcher.fullScan();

    verify(profileRegistry).register(p1);
    verify(profileRegistry).register(p2);
  }

  @Test
  @DisplayName("onAgentDirChanged with ENTRY_CREATE registers the agent")
  void onAgentDirChanged_create_registersAgent() throws Exception {
    Path agentDir = agentsDir.resolve("new-agent");
    Files.createDirectories(agentDir);

    Profile profile = buildProfile("new-agent");
    when(agentLoader.deriveProfile(agentDir)).thenReturn(profile);

    watcher.onAgentDirChanged(agentDir, java.nio.file.StandardWatchEventKinds.ENTRY_CREATE);

    verify(profileRegistry).register(profile);
  }

  @Test
  @DisplayName("onAgentDirChanged with ENTRY_DELETE unregisters the agent")
  void onAgentDirChanged_delete_unregistersAgent() {
    Path agentDir = agentsDir.resolve("gone-agent");
    Profile profile = buildProfile("gone-agent");

    when(profileRegistry.get("gone-agent")).thenReturn(profile);

    watcher.onAgentDirChanged(agentDir, java.nio.file.StandardWatchEventKinds.ENTRY_DELETE);

    verify(agentScheduler).unregisterProfile(profile);
    verify(profileRegistry).remove("gone-agent");
  }

  @Test
  @DisplayName("watchLoop 收到目录删除事件后注销 Agent（删除时目录已不存在，不能被 isDirectory 过滤掉）")
  void watchLoop_unregistersOnDirDelete() throws Exception {
    Path agentDir = agentsDir.resolve("temp-agent");
    Files.createDirectories(agentDir);
    Files.writeString(agentDir.resolve("AGENT.md"), VALID_AGENT_MD);
    Profile profile = buildProfile("temp-agent");
    when(agentLoader.deriveProfile(agentDir)).thenReturn(profile);
    when(profileRegistry.get("temp-agent")).thenReturn(profile);

    watcher.start();
    try {
      verify(profileRegistry, timeout(3000)).register(profile);

      // watch 线程是异步起来的，等它把目录注册进 WatchService 再删，否则事件根本不会产生
      Thread.sleep(300);
      Files.delete(agentDir.resolve("AGENT.md"));
      Files.delete(agentDir);

      verify(profileRegistry, timeout(5000)).remove("temp-agent");
    } finally {
      watcher.stop();
    }
  }

  @Test
  @DisplayName("onAgentDirChanged does not crash when deriveProfile fails")
  void onAgentDirChanged_badDir_doesNotCrash() throws Exception {
    Path agentDir = agentsDir.resolve("bad-agent");
    Files.createDirectories(agentDir);

    when(agentLoader.deriveProfile(agentDir))
        .thenThrow(new IllegalArgumentException("bad AGENT.md"));

    watcher.onAgentDirChanged(agentDir, java.nio.file.StandardWatchEventKinds.ENTRY_CREATE);

    verify(profileRegistry, never()).register(any(Profile.class));
  }
}
