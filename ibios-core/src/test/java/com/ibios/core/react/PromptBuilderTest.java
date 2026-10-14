package com.ibios.core.react;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ibios.core.port.ChatPort.Prompt;
import com.ibios.core.profile.Profile;
import com.ibios.core.session.Session;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PromptBuilderTest {

  private ContextLoader contextLoader;
  private PromptBuilder promptBuilder;

  @BeforeEach
  void setUp() {
    contextLoader = new ContextLoader();
    promptBuilder = new PromptBuilder(contextLoader);
  }

  @Test
  void shouldBuildPromptWithCorrectOrder() {
    Session session = new Session("s1", "test-profile", "cli", "test-user");
    session.appendUserMessage("Hello");
    Profile profile = createProfile("You are helpful", List.of(), List.of());

    Prompt prompt = promptBuilder.build(session, profile);

    assertTrue(prompt.systemPrompt().contains("You are helpful"));
    assertTrue(prompt.systemPrompt().contains("Current date and time:"));
    assertEquals(1, prompt.messages().size());
    assertEquals("Hello", prompt.messages().get(0).content());
  }

  @Test
  void shouldTruncateHistoryWhenExceedsMaxTurns() {
    Session session = new Session("s1", "test-profile", "cli", "test-user");
    for (int i = 0; i < 30; i++) {
      session.appendUserMessage("Message " + i);
    }
    Profile profile = createProfile("prompt", List.of(), List.of());

    Prompt prompt = promptBuilder.build(session, profile);

    assertTrue(prompt.messages().size() <= 40);
  }

  @Test
  void shouldIncludeCurrentDateTimeInSystemPrompt() {
    Session session = new Session("s1", "test-profile", "cli", "test-user");
    Profile profile = createProfile("prompt", List.of(), List.of());

    Prompt prompt = promptBuilder.build(session, profile);

    assertTrue(prompt.systemPrompt().contains("Current date and time:"));
  }

  private Profile createProfile(String systemPrompt, List<String> bootstrap, List<String> skills) {
    return new Profile(
        "test",
        "test profile",
        new Profile.Identity("agent", systemPrompt),
        new Profile.ProviderConfig("dashscope", "qwen-plus", null),
        List.of(),
        skills,
        List.of(),
        List.of(),
        List.of(),
        List.of(),
        bootstrap,
        Map.of(),
        10,
        20);
  }
}
