package com.ibios.core.profile;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ProfileRegistryRuntimeTest {

  private ProfileRegistry registry;

  @BeforeEach
  void setUp() {
    registry = new ProfileRegistry();
  }

  @Test
  @DisplayName("register 后立即 get 可见")
  void registerThenGetVisible() {
    Profile profile = createProfile("test-agent");
    registry.register(profile);
    assertSame(profile, registry.get("test-agent"));
  }

  @Test
  @DisplayName("remove 后 get 返回 null、exists 返回 false")
  void removeThenGone() {
    registry.register(createProfile("test-agent"));
    assertTrue(registry.exists("test-agent"));

    registry.remove("test-agent");

    assertNull(registry.get("test-agent"));
    assertFalse(registry.exists("test-agent"));
  }

  @Test
  @DisplayName("exists 对已注册返回 true")
  void existsReturnsTrueForRegistered() {
    registry.register(createProfile("test-agent"));
    assertTrue(registry.exists("test-agent"));
  }

  @Test
  @DisplayName("exists 对未注册返回 false")
  void existsReturnsFalseForUnregistered() {
    assertFalse(registry.exists("nonexistent"));
  }

  @Test
  @DisplayName("remove 不存在的 key 不报错")
  void removeNonexistentDoesNotThrow() {
    assertDoesNotThrow(() -> registry.remove("nonexistent"));
  }

  private Profile createProfile(String name) {
    return new Profile(
        name,
        "test profile",
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
        null);
  }
}
