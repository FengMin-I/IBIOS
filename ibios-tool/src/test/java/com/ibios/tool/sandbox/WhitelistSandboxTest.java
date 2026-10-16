package com.ibios.tool.sandbox;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ibios.core.tool.SandboxAction;
import com.ibios.core.tool.SandboxAction.ActionType;
import com.ibios.core.tool.SandboxViolationException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class WhitelistSandboxTest {

  private WhitelistSandbox sandbox;

  @BeforeEach
  void setUp() {
    WhitelistManager manager =
        new WhitelistManager(
            new FileSandboxProperties(List.of("/workspace")),
            new ShellSandboxProperties(List.of("ls", "cat", "echo")),
            new HttpSandboxProperties(List.of("api.weather.com", "*.example.com")));
    sandbox = new WhitelistSandbox(manager);
  }

  @Test
  @DisplayName("文件路径：白名单内放行")
  void filePathAllowed() {
    assertDoesNotThrow(
        () -> sandbox.enforce(new SandboxAction(ActionType.FILE_READ, "/workspace/data.txt")));
  }

  @Test
  @DisplayName("文件路径：白名单外拒绝")
  void filePathDenied() {
    SandboxViolationException ex =
        assertThrows(
            SandboxViolationException.class,
            () -> sandbox.enforce(new SandboxAction(ActionType.FILE_READ, "/etc/passwd")));
    assertTrue(ex.getMessage().contains("/etc/passwd"));
  }

  @Test
  @DisplayName("文件路径：相对路径穿越（.. 爬出白名单）被拦")
  void pathTraversalBlocked() {
    assertThrows(
        SandboxViolationException.class,
        () ->
            sandbox.enforce(
                new SandboxAction(ActionType.FILE_READ, "/workspace/../../outside/secret.txt")));
  }

  @Test
  @DisplayName("Shell 命令：白名单内放行")
  void shellCommandAllowed() {
    assertDoesNotThrow(
        () -> sandbox.enforce(new SandboxAction(ActionType.SHELL_COMMAND, "ls -la /workspace")));
  }

  @Test
  @DisplayName("Shell 命令：白名单外拒绝")
  void shellCommandDenied() {
    SandboxViolationException ex =
        assertThrows(
            SandboxViolationException.class,
            () -> sandbox.enforce(new SandboxAction(ActionType.SHELL_COMMAND, "rm -rf /")));
    assertTrue(ex.getMessage().contains("rm"));
  }

  @Test
  @DisplayName("Shell 命令：首 token 带前导空格仍正确提取")
  void shellCommandLeadingWhitespace() {
    assertThrows(
        SandboxViolationException.class,
        () -> sandbox.enforce(new SandboxAction(ActionType.SHELL_COMMAND, "  rm -rf /")));
  }

  @Test
  @DisplayName("HTTP 域名：精确匹配放行")
  void httpExactMatchAllowed() {
    assertDoesNotThrow(
        () ->
            sandbox.enforce(
                new SandboxAction(ActionType.HTTP_REQUEST, "https://api.weather.com/forecast")));
  }

  @Test
  @DisplayName("HTTP 域名：白名单外拒绝")
  void httpDomainDenied() {
    assertThrows(
        SandboxViolationException.class,
        () ->
            sandbox.enforce(new SandboxAction(ActionType.HTTP_REQUEST, "https://evil.com/steal")));
  }

  @Test
  @DisplayName("HTTP 域名：通配符 *.example.com 命中 api.example.com")
  void httpWildcardSubdomainMatch() {
    assertDoesNotThrow(
        () ->
            sandbox.enforce(
                new SandboxAction(ActionType.HTTP_REQUEST, "https://api.example.com/data")));
  }

  @Test
  @DisplayName("HTTP 域名：通配符 *.example.com 不命中 evil-example.com（点号边界）")
  void httpWildcardDotBoundary() {
    assertThrows(
        SandboxViolationException.class,
        () ->
            sandbox.enforce(
                new SandboxAction(ActionType.HTTP_REQUEST, "https://evil-example.com/x")));
  }

  @Test
  @DisplayName("空配置 = 全部拒绝：文件路径")
  void emptyConfigDeniesAllFile() {
    WhitelistManager mgr =
        new WhitelistManager(
            new FileSandboxProperties(List.of()),
            new ShellSandboxProperties(List.of("ls")),
            new HttpSandboxProperties(List.of("*")));
    WhitelistSandbox empty = new WhitelistSandbox(mgr);
    assertThrows(
        SandboxViolationException.class,
        () -> empty.enforce(new SandboxAction(ActionType.FILE_READ, "/any/path")));
  }

  @Test
  @DisplayName("空配置 = 全部拒绝：Shell 命令")
  void emptyConfigDeniesAllShell() {
    WhitelistManager mgr =
        new WhitelistManager(
            new FileSandboxProperties(List.of("/tmp")),
            new ShellSandboxProperties(List.of()),
            new HttpSandboxProperties(List.of("*")));
    WhitelistSandbox empty = new WhitelistSandbox(mgr);
    assertThrows(
        SandboxViolationException.class,
        () -> empty.enforce(new SandboxAction(ActionType.SHELL_COMMAND, "ls")));
  }

  @Test
  @DisplayName("通配符 * 允许所有域名")
  void wildcardStarAllowsAll() {
    WhitelistManager mgr =
        new WhitelistManager(
            new FileSandboxProperties(List.of("/tmp")),
            new ShellSandboxProperties(List.of("ls")),
            new HttpSandboxProperties(List.of("*")));
    WhitelistSandbox permissive = new WhitelistSandbox(mgr);
    assertDoesNotThrow(
        () ->
            permissive.enforce(
                new SandboxAction(ActionType.HTTP_REQUEST, "https://any-domain.org/path")));
  }
}
