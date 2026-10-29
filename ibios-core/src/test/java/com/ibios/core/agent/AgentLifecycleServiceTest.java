package com.ibios.core.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ibios.core.port.ChatPort;
import com.ibios.core.profile.Profile;
import com.ibios.core.profile.ProfileRegistry;
import com.ibios.core.react.AgentScheduler;
import com.ibios.core.session.SessionManager;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AgentLifecycleServiceTest {

  @Mock private AgentStore agentStore;
  @Mock private AgentLoader agentLoader;
  @Mock private ProfileRegistry profileRegistry;
  @Mock private AgentScheduler agentScheduler;
  @Mock private ChatPort chatPort;
  @Mock private SessionManager sessionManager;

  @TempDir Path tempDir;

  private AgentLifecycleService service;

  @BeforeEach
  void setUp() {
    lenient().when(agentStore.agentsDir()).thenReturn(tempDir);
    service =
        new AgentLifecycleService(
            agentStore,
            agentLoader,
            profileRegistry,
            agentScheduler,
            chatPort,
            sessionManager,
            () -> Map.of(),
            Set.of("dashscope", "openai")::contains,
            "dashscope",
            "qwen-plus",
            "You are an agent author.");
  }

  private Profile createProfileWithSchedule(Path agentDir) {
    return new Profile(
        "test-agent",
        "A test agent",
        new Profile.Identity("agent", "prompt"),
        new Profile.ProviderConfig("dashscope", "qwen-plus", null),
        List.of(),
        List.of(),
        List.of(),
        List.of(new Profile.ScheduleConfig("sched-1", "0 9 * * *", "UTC", "daily run")),
        List.of(),
        Map.of(),
        10,
        20,
        agentDir);
  }

  private Profile createProfileNoSchedule(Path agentDir) {
    return new Profile(
        "test-agent",
        "A test agent",
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
        agentDir);
  }

  @Test
  @DisplayName(
      "create calls dependencies in order: writeAll, deriveProfile, register, registerProfile")
  void create_executesInOrder() {
    Path agentDir = tempDir.resolve("test-agent");
    Profile profile = createProfileWithSchedule(agentDir);

    when(profileRegistry.exists("test-agent")).thenReturn(false);
    when(agentStore.writeAll(eq("test-agent"), anyMap())).thenReturn(agentDir);
    when(agentLoader.deriveProfile(agentDir)).thenReturn(profile);

    service.create("test-agent", "A test agent");

    InOrder order = inOrder(agentStore, agentLoader, profileRegistry, agentScheduler);
    order.verify(agentStore).writeAll(eq("test-agent"), anyMap());
    order.verify(agentLoader).deriveProfile(agentDir);
    order.verify(profileRegistry).register(profile);
    order.verify(agentScheduler).registerProfile(profile);
  }

  @Test
  @DisplayName("create rejects name conflict before writing anything")
  void create_nameConflict_rejectsBeforeWriting() {
    when(profileRegistry.exists("test-agent")).thenReturn(true);

    assertThrows(IllegalArgumentException.class, () -> service.create("test-agent", "desc"));

    verify(agentStore, never()).writeAll(anyString(), anyMap());
  }

  @Test
  @DisplayName("create rolls back agent directory when register fails")
  void create_registerFails_rollsBackAgentDir() {
    Path agentDir = tempDir.resolve("test-agent");

    when(profileRegistry.exists("test-agent")).thenReturn(false);
    when(agentStore.writeAll(eq("test-agent"), anyMap())).thenReturn(agentDir);
    when(agentLoader.deriveProfile(agentDir)).thenReturn(createProfileNoSchedule(agentDir));
    doThrow(new RuntimeException("register failed"))
        .when(profileRegistry)
        .register(any(Profile.class));

    assertThrows(IllegalStateException.class, () -> service.create("test-agent", "desc"));

    verify(agentStore).delete(agentDir);
  }

  @Test
  @DisplayName("delete follows correct order: unregister, remove, archive")
  void delete_followsCorrectOrder() {
    Path agentDir = tempDir.resolve("test-agent");
    Profile profile = createProfileNoSchedule(agentDir);

    when(profileRegistry.get("test-agent")).thenReturn(profile);

    service.delete("test-agent");

    InOrder order = inOrder(agentScheduler, profileRegistry, agentStore);
    order.verify(agentScheduler).unregisterProfile(profile);
    order.verify(profileRegistry).remove("test-agent");
    order.verify(agentStore).archive("test-agent");
  }

  @Test
  @DisplayName("update with schedule changes unregisters before re-registering")
  void update_withScheduleChanges_unregistersFirst() {
    Path agentsDir = tempDir;
    Path agentDir = agentsDir.resolve("test-agent");

    Profile existing =
        new Profile(
            "test-agent",
            "desc",
            new Profile.Identity("agent", "prompt"),
            new Profile.ProviderConfig("dashscope", "qwen-plus", null),
            List.of(),
            List.of(),
            List.of(),
            List.of(new Profile.ScheduleConfig("sched-1", "0 9 * * *", "UTC", "daily")),
            List.of(),
            Map.of(),
            10,
            20,
            agentDir);

    Profile newProfile = createProfileNoSchedule(agentDir);

    when(profileRegistry.get("test-agent")).thenReturn(existing);
    when(agentLoader.deriveProfile(agentDir)).thenReturn(newProfile);

    // Create the agent dir so Files.writeString in update() succeeds
    try {
      java.nio.file.Files.createDirectories(agentDir);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    service.update(
        "test-agent",
        "---\nname: test-agent\nprovider:\n  name: dashscope\n  model: qwen-plus\n---\nBody");

    InOrder order = inOrder(agentScheduler, agentLoader);
    order.verify(agentScheduler).unregisterProfile(existing);
    order.verify(agentLoader).deriveProfile(agentDir);
  }

  @Test
  @DisplayName("list 为每个 Agent 带出历史实际调用过的工具")
  void list_fillsUsedTools() {
    when(profileRegistry.getAll()).thenReturn(List.of(createProfileNoSchedule(tempDir)));

    List<AgentView> views =
        serviceWithUsage(Map.of("test-agent", List.of("http_get", "notify"))).list();

    assertEquals(List.of("http_get", "notify"), views.get(0).usedTools());
  }

  @Test
  @DisplayName("list 对没有历史记录的 Agent 返回空工具列表")
  void list_withoutHistory_returnsEmptyTools() {
    when(profileRegistry.getAll()).thenReturn(List.of(createProfileNoSchedule(tempDir)));

    List<AgentView> views = serviceWithUsage(Map.of()).list();

    assertTrue(views.get(0).usedTools().isEmpty());
  }

  private AgentLifecycleService serviceWithUsage(Map<String, List<String>> usage) {
    return new AgentLifecycleService(
        agentStore,
        agentLoader,
        profileRegistry,
        agentScheduler,
        chatPort,
        sessionManager,
        () -> usage,
        Set.of("dashscope", "openai")::contains,
        "dashscope",
        "qwen-plus",
        "You are an agent author.");
  }

  @Test
  @DisplayName("generateFiles returns valid markdown with AGENT.md key")
  void generateFiles_returnsValidMarkdown() {
    String validMd =
        "---\nname: generated-agent\nprovider:\n  name: dashscope\n  model: qwen-plus\n---\n# Generated Agent\nDo something useful.";

    when(profileRegistry.exists("gen-agent")).thenReturn(false);
    when(chatPort.chat(anyString(), any(Profile.class), any(ChatPort.Prompt.class)))
        .thenReturn(new ChatPort.ChatResponse(validMd, List.of()));
    when(agentLoader.deriveProfile(any(Path.class))).thenReturn(null);

    Map<String, String> result = service.generateFiles("gen-agent", "A useful agent");

    assertTrue(result.containsKey("AGENT.md"));
    assertEquals(validMd, result.get("AGENT.md"));
  }

  @Test
  @DisplayName("generateFiles throws when LLM returns invalid content")
  void generateFiles_invalidLlmOutput_throws() {
    when(profileRegistry.exists("gen-agent")).thenReturn(false);
    when(chatPort.chat(anyString(), any(Profile.class), any(ChatPort.Prompt.class)))
        .thenReturn(new ChatPort.ChatResponse("not valid yaml", List.of()));
    when(agentLoader.deriveProfile(any(Path.class)))
        .thenThrow(new IllegalArgumentException("missing frontmatter"));

    assertThrows(IllegalStateException.class, () -> service.generateFiles("gen-agent", "desc"));
  }
}
