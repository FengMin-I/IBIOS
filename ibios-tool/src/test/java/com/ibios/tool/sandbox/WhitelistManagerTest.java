package com.ibios.tool.sandbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WhitelistManagerTest {

  private WhitelistManager manager;

  @BeforeEach
  void setUp() {
    manager =
        new WhitelistManager(
            new FileSandboxProperties(List.of("/workspace")),
            new ShellSandboxProperties(List.of("ls", "cat")),
            new HttpSandboxProperties(List.of("api.weather.com")));
  }

  @Test
  @DisplayName("初始化：从配置加载白名单")
  void initFromConfig() {
    WhitelistManager.WhitelistSnapshot snap = manager.snapshot();
    assertEquals(1, snap.filePaths().size());
    assertEquals(2, snap.shellCommands().size());
    assertEquals(1, snap.httpDomains().size());
  }

  @Test
  @DisplayName("增加文件路径：新增后生效")
  void addFilePath() {
    manager.addFilePath("/data");
    assertTrue(manager.snapshot().filePaths().contains("/data"));
  }

  @Test
  @DisplayName("增加文件路径：重复增加不重复")
  void addFilePathDuplicate() {
    manager.addFilePath("/workspace");
    assertEquals(1, manager.snapshot().filePaths().size());
  }

  @Test
  @DisplayName("删除文件路径：存在则删除")
  void removeFilePath() {
    assertTrue(manager.removeFilePath("/workspace"));
    assertFalse(manager.snapshot().filePaths().contains("/workspace"));
  }

  @Test
  @DisplayName("删除文件路径：不存在返回 false")
  void removeFilePathNotFound() {
    assertFalse(manager.removeFilePath("/nonexistent"));
  }

  @Test
  @DisplayName("增加 Shell 命令：新增后生效")
  void addShellCommand() {
    manager.addShellCommand("echo");
    assertTrue(manager.snapshot().shellCommands().contains("echo"));
  }

  @Test
  @DisplayName("删除 Shell 命令：存在则删除")
  void removeShellCommand() {
    assertTrue(manager.removeShellCommand("ls"));
    assertFalse(manager.snapshot().shellCommands().contains("ls"));
  }

  @Test
  @DisplayName("增加 HTTP 域名：新增后生效")
  void addHttpDomain() {
    manager.addHttpDomain("api.openai.com");
    assertTrue(manager.snapshot().httpDomains().contains("api.openai.com"));
  }

  @Test
  @DisplayName("删除 HTTP 域名：存在则删除")
  void removeHttpDomain() {
    assertTrue(manager.removeHttpDomain("api.weather.com"));
    assertFalse(manager.snapshot().httpDomains().contains("api.weather.com"));
  }

  @Test
  @DisplayName("动态增加后 Sandbox 立即生效")
  void dynamicAddTakesEffectImmediately() {
    WhitelistSandbox sandbox = new WhitelistSandbox(manager);
    manager.addFilePath("/data");
    org.junit.jupiter.api.Assertions.assertDoesNotThrow(
        () ->
            sandbox.enforce(
                new com.ibios.core.tool.SandboxAction(
                    com.ibios.core.tool.SandboxAction.ActionType.FILE_READ, "/data/test.txt")));
  }
}
