package com.ibios.core.workspace;

import java.nio.file.Path;

/**
 * 工作区根的统一解析入口：轻命令（不启动 Spring）按 {@code -Dibios.root} → {@code IBIOS_ROOT} → 默认 {@code .ibios}
 * 的顺序取根，重命令从 Spring 属性 {@code ibios.root} 取根后构造同一个对象。
 */
public final class WorkspaceRoot {

  public static final String DEFAULT_ROOT = ".ibios";
  public static final String SYSTEM_PROPERTY = "ibios.root";
  public static final String ENV_VAR = "IBIOS_ROOT";

  private final Path root;

  public WorkspaceRoot(Path root) {
    this.root = root;
  }

  public WorkspaceRoot(String root) {
    this(Path.of(root));
  }

  /** 供轻命令使用：JVM 系统属性优先于环境变量，都没有则用默认根。 */
  public static WorkspaceRoot resolve() {
    String fromProperty = System.getProperty(SYSTEM_PROPERTY);
    if (fromProperty != null && !fromProperty.isBlank()) {
      return new WorkspaceRoot(fromProperty.trim());
    }
    String fromEnv = System.getenv(ENV_VAR);
    if (fromEnv != null && !fromEnv.isBlank()) {
      return new WorkspaceRoot(fromEnv.trim());
    }
    return new WorkspaceRoot(DEFAULT_ROOT);
  }

  public Path path() {
    return root;
  }

  public Path agentsDir() {
    return root.resolve("agents");
  }

  public Path archiveDir() {
    return root.resolve("archive");
  }

  public Path profilesDir() {
    return root.resolve("profiles");
  }

  public Path skillsDir() {
    return root.resolve("skills");
  }

  public Path mcpConfigFile() {
    return root.resolve("mcp_servers.yaml");
  }

  public Path memoryFile() {
    return root.resolve("memory").resolve("MEMORY.md");
  }
}
