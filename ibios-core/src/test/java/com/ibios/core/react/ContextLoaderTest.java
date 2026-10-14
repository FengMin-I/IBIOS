package com.ibios.core.react;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ibios.core.profile.Profile;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ContextLoaderTest {

  @TempDir Path tempDir;

  private ContextLoader contextLoader;

  @BeforeEach
  void setUp() {
    contextLoader = new ContextLoader();
  }

  @Test
  void shouldReadFileContentWithoutCaching() throws IOException {
    Path bootstrapFile = tempDir.resolve("bootstrap.md");
    Files.writeString(bootstrapFile, "Initial content");

    Profile profile = createProfile(List.of(bootstrapFile.toString()), List.of());
    String content1 = contextLoader.loadBootstrapContent(profile);
    assertEquals("Initial content", content1.trim());

    Files.writeString(bootstrapFile, "Updated content");
    String content2 = contextLoader.loadBootstrapContent(profile);
    assertEquals("Updated content", content2.trim());
  }

  @Test
  void shouldThrowExceptionWhenSkillFileNotFound() {
    Profile profile = createProfile(List.of(), List.of("/nonexistent/skill.md"));

    assertThrows(IllegalStateException.class, () -> contextLoader.loadSkillContent(profile));
  }

  @Test
  void shouldLogWarningWhenBootstrapFileNotFound() {
    Profile profile = createProfile(List.of("/nonexistent/bootstrap.md"), List.of());

    String content = contextLoader.loadBootstrapContent(profile);

    assertTrue(content.isEmpty());
  }

  private Profile createProfile(List<String> bootstrap, List<String> skills) {
    return new Profile(
        "test",
        "test profile",
        new Profile.Identity("agent", "prompt"),
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
