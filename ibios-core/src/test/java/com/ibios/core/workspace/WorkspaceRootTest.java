package com.ibios.core.workspace;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WorkspaceRootTest {

  @Test
  @DisplayName("resolve 优先取 -Dibios.root")
  void resolvePrefersSystemProperty() {
    String original = System.getProperty(WorkspaceRoot.SYSTEM_PROPERTY);
    try {
      System.setProperty(WorkspaceRoot.SYSTEM_PROPERTY, "/data/ws");
      assertEquals(Path.of("/data/ws"), WorkspaceRoot.resolve().path());
    } finally {
      restoreProperty(original);
    }
  }

  @Test
  @DisplayName("未配置时回落到默认根")
  void resolveFallsBackToDefault() {
    String original = System.getProperty(WorkspaceRoot.SYSTEM_PROPERTY);
    try {
      restoreProperty(null);
      if (System.getenv(WorkspaceRoot.ENV_VAR) == null) {
        assertEquals(Path.of(WorkspaceRoot.DEFAULT_ROOT), WorkspaceRoot.resolve().path());
      }
    } finally {
      restoreProperty(original);
    }
  }

  @Test
  @DisplayName("子目录按根派生")
  void derivesSubdirectories() {
    WorkspaceRoot root = new WorkspaceRoot("/data/ws");

    assertEquals(Path.of("/data/ws/agents"), root.agentsDir());
    assertEquals(Path.of("/data/ws/archive"), root.archiveDir());
    assertEquals(Path.of("/data/ws/profiles"), root.profilesDir());
    assertEquals(Path.of("/data/ws/skills"), root.skillsDir());
    assertEquals(Path.of("/data/ws/mcp_servers.yaml"), root.mcpConfigFile());
    assertEquals(Path.of("/data/ws/memory/MEMORY.md"), root.memoryFile());
  }

  private static void restoreProperty(String value) {
    if (value == null) {
      System.clearProperty(WorkspaceRoot.SYSTEM_PROPERTY);
    } else {
      System.setProperty(WorkspaceRoot.SYSTEM_PROPERTY, value);
    }
  }
}
