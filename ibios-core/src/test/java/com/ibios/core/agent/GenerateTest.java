package com.ibios.core.agent;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GenerateTest {

  @Mock private AgentStore agentStore;
  @Mock private AgentLoader agentLoader;
  @Mock private ProfileRegistry profileRegistry;
  @Mock private AgentScheduler agentScheduler;
  @Mock private ChatPort chatPort;
  @Mock private SessionManager sessionManager;

  @TempDir Path tempDir;

  private AgentLifecycleService service;

  private static final String VALID_AGENT_MD =
      "---\nname: generated-agent\nprovider:\n  name: dashscope\n  model: qwen-plus\n---\n# Generated Agent\nDo something useful.";

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
            Set.of("dashscope")::contains,
            "dashscope",
            "qwen-plus",
            "You are an agent author.");
  }

  @Test
  @DisplayName("generateFiles produces parseable markdown containing AGENT.md key")
  void generateFiles_producesParseableMarkdown() {
    when(profileRegistry.exists("gen-agent")).thenReturn(false);
    when(chatPort.chat(anyString(), any(Profile.class), any(ChatPort.Prompt.class)))
        .thenReturn(new ChatPort.ChatResponse(VALID_AGENT_MD, List.of()));
    when(agentLoader.deriveProfile(any(Path.class))).thenReturn(null);

    Map<String, String> result = service.generateFiles("gen-agent", "A useful agent");

    assertTrue(result.containsKey("AGENT.md"));
    String content = result.get("AGENT.md");
    assertTrue(content.contains("name: generated-agent"));
    assertTrue(content.contains("Do something useful."));
  }

  @Test
  @DisplayName("generateFiles does not write to store or register in profile registry")
  void generateFiles_doesNotWriteOrRegister() {
    when(profileRegistry.exists("gen-agent")).thenReturn(false);
    when(chatPort.chat(anyString(), any(Profile.class), any(ChatPort.Prompt.class)))
        .thenReturn(new ChatPort.ChatResponse(VALID_AGENT_MD, List.of()));
    when(agentLoader.deriveProfile(any(Path.class))).thenReturn(null);

    service.generateFiles("gen-agent", "desc");

    verify(agentStore, never()).writeAll(anyString(), any(Map.class));
    verify(profileRegistry, never()).register(any(Profile.class));
  }

  @Test
  @DisplayName("generateFiles throws IllegalStateException when LLM output is invalid")
  void generateFiles_invalidOutput_throws() {
    when(profileRegistry.exists("gen-agent")).thenReturn(false);
    when(chatPort.chat(anyString(), any(Profile.class), any(ChatPort.Prompt.class)))
        .thenReturn(new ChatPort.ChatResponse("not valid yaml", List.of()));
    when(agentLoader.deriveProfile(any(Path.class)))
        .thenThrow(new IllegalArgumentException("missing frontmatter"));

    assertThrows(IllegalStateException.class, () -> service.generateFiles("gen-agent", "desc"));
  }
}
