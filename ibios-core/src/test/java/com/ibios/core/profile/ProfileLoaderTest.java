package com.ibios.core.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProfileLoaderTest {

  private ProfileRegistry registry;

  @BeforeEach
  void setUp() {
    registry = new ProfileRegistry();
  }

  @Test
  void shouldParseFullFieldProfile() {
    ProfileLoader loader = new ProfileLoader(registry, Set.of("dashscope"));
    Path dir = testProfilesDir();

    loader.loadAll(dir);

    Profile profile = registry.get("test-agent");
    assertNotNull(profile);
    assertEquals("test-agent", profile.name());
    assertEquals("A test agent profile", profile.description());
    assertEquals("TestAgent", profile.identity().agentName());
    assertEquals("You are a helpful test assistant.", profile.identity().prompt());
    assertEquals("dashscope", profile.provider().name());
    assertEquals("qwen-plus", profile.provider().model());
    assertEquals(0.7, profile.provider().temperature(), 0.001);
    assertEquals(2, profile.tools().size());
    assertTrue(profile.tools().contains("web_search"));
    assertTrue(profile.tools().contains("calculator"));
    assertEquals(1, profile.skills().size());
    assertEquals(1, profile.mcpServers().size());
    assertEquals("fs-server", profile.mcpServers().get(0));
    assertEquals(1, profile.channels().size());
    assertEquals(1, profile.notifyChannels().size());
    assertEquals(1, profile.schedules().size());
    assertEquals("0 9 * * *", profile.schedules().get(0).cron());
    assertEquals("Asia/Shanghai", profile.schedules().get(0).zone());
    assertEquals("Good morning!", profile.schedules().get(0).message());
    assertEquals(1, profile.bootstrap().size());
    assertEquals(Map.of("max_retries", 3, "debug", true), profile.settings());
  }

  @Test
  void shouldThrowForUnknownProvider() {
    ProfileLoader loader = new ProfileLoader(registry, Set.of("dashscope"));
    Path dir = testProfilesDir();

    loader.loadAll(dir);

    assertNull(registry.get("unknown-provider-agent"));
  }

  @Test
  void shouldNotBlockOtherProfilesOnBadFile() {
    ProfileLoader loader = new ProfileLoader(registry, Set.of("dashscope"));
    Path dir = testProfilesDir();

    loader.loadAll(dir);

    assertNotNull(registry.get("test-agent"));
    assertNull(registry.get("broken-agent"));
    assertNull(registry.get("unknown-provider-agent"));
  }

  @Test
  void shouldResolveEnvVars() {
    String pathVar = System.getenv("PATH");
    assertNotNull(pathVar);
    String input = "value: ${PATH}";
    String resolved = ProfileLoader.resolveEnvVars(input);
    assertEquals("value: " + pathVar, resolved);
  }

  @Test
  void shouldThrowOnMissingEnvVar() {
    String input = "value: ${IBIOS_NONEXISTENT_VAR_12345}";
    assertThrows(IllegalStateException.class, () -> ProfileLoader.resolveEnvVars(input));
  }

  private Path testProfilesDir() {
    return Paths.get("src/test/resources/profiles");
  }
}
