package com.ibios.core.agent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
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
            agentsDir, agentLoader, profileRegistry, agentScheduler, Set.of("dashscope"));
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
