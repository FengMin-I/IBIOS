package com.ibios.tool.sandbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ibios.core.sandbox.WhitelistEntryView;
import com.ibios.core.sandbox.WhitelistType;
import com.ibios.tool.sandbox.WhitelistManager.DeleteOutcome;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WhitelistManagerTest {

  private InMemorySandboxWhitelistStore store;
  private WhitelistManager manager;

  @BeforeEach
  void setUp() {
    store = new InMemorySandboxWhitelistStore();
    manager = createManager(".ibios");
  }

  private WhitelistManager createManager(String root) {
    WhitelistManager created =
        new WhitelistManager(
            new FileSandboxProperties(List.of("/workspace")),
            new ShellSandboxProperties(List.of("ls", "cat")),
            new HttpSandboxProperties(List.of("api.weather.com")),
            store,
            root);
    created.initialize();
    return created;
  }

  @Test
  @DisplayName("初始化：配置项播种进 store，工作区根自动补齐")
  void initSeedsConfigIntoStore() {
    WhitelistManager.WhitelistSnapshot snap = manager.snapshot();

    assertTrue(snap.filePaths().contains("/workspace"));
    assertTrue(snap.filePaths().contains(".ibios"));
    assertEquals(2, snap.shellCommands().size());
    assertEquals(1, snap.httpDomains().size());
    assertEquals(5, manager.entries().size());
  }

  @Test
  @DisplayName("启动播种幂等：重复 initialize 不产生重复行")
  void seedingIsIdempotent() {
    manager.initialize();

    assertEquals(5, manager.entries().size());
  }

  @Test
  @DisplayName("增加文件路径：写穿到 store 并立即生效")
  void addFilePathWritesThrough() {
    assertTrue(manager.addFilePath("/data"));

    assertTrue(manager.snapshot().filePaths().contains("/data"));
    assertTrue(store.exists(WhitelistType.FILE, "/data"));
  }

  @Test
  @DisplayName("增加文件路径：重复增加返回 false 且不重复")
  void addFilePathDuplicate() {
    assertFalse(manager.addFilePath("/workspace"));
    assertEquals(1, manager.entries().stream().filter(e -> e.value().equals("/workspace")).count());
  }

  @Test
  @DisplayName("删除文件路径：存在则删除并落库")
  void removeFilePath() {
    assertTrue(manager.removeFilePath("/workspace"));
    assertFalse(manager.snapshot().filePaths().contains("/workspace"));
    assertFalse(store.exists(WhitelistType.FILE, "/workspace"));
  }

  @Test
  @DisplayName("删除文件路径：不存在返回 false")
  void removeFilePathNotFound() {
    assertFalse(manager.removeFilePath("/nonexistent"));
  }

  @Test
  @DisplayName("工作区根受保护：按 id 删除返回 PROTECTED")
  void workspaceRootIsProtected() {
    WhitelistEntryView rootEntry =
        manager.entries().stream()
            .filter(e -> e.type() == WhitelistType.FILE && e.value().equals(".ibios"))
            .findFirst()
            .orElseThrow();

    assertEquals(DeleteOutcome.PROTECTED, manager.removeById(rootEntry.id()));
    assertTrue(manager.snapshot().filePaths().contains(".ibios"));
  }

  @Test
  @DisplayName("删除不存在的 id 返回 NOT_FOUND")
  void removeByIdNotFound() {
    assertEquals(DeleteOutcome.NOT_FOUND, manager.removeById(9999L));
  }

  @Test
  @DisplayName("重启后从 store 恢复运行时新增项")
  void restartRestoresFromStore() {
    manager.addFilePath("/data");

    WhitelistManager restarted = createManager(".ibios");

    assertTrue(restarted.snapshot().filePaths().contains("/data"));
  }

  @Test
  @DisplayName("增加 Shell 命令：新增后生效")
  void addShellCommand() {
    assertTrue(manager.addShellCommand("echo"));
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
    assertTrue(manager.addHttpDomain("api.openai.com"));
    assertTrue(manager.snapshot().httpDomains().contains("api.openai.com"));
  }

  @Test
  @DisplayName("删除 HTTP 域名：存在则删除")
  void removeHttpDomain() {
    assertTrue(manager.removeHttpDomain("api.weather.com"));
    assertFalse(manager.snapshot().httpDomains().contains("api.weather.com"));
  }

  @Test
  @DisplayName("entries 暴露带 id 的条目供管理台使用")
  void entriesExposeIds() {
    List<WhitelistEntryView> entries = manager.entries();

    assertFalse(entries.isEmpty());
    assertNotNull(entries.get(0).id());
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
