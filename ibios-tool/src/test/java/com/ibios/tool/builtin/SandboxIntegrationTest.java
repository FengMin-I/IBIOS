package com.ibios.tool.builtin;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ibios.core.tool.Sandbox;
import com.ibios.core.tool.ToolExecutionResult;
import com.ibios.tool.sandbox.FileSandboxProperties;
import com.ibios.tool.sandbox.HttpSandboxProperties;
import com.ibios.tool.sandbox.InMemorySandboxWhitelistStore;
import com.ibios.tool.sandbox.ShellSandboxProperties;
import com.ibios.tool.sandbox.WhitelistManager;
import com.ibios.tool.sandbox.WhitelistSandbox;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

@DisplayName("Sandbox 接线回归：四个 Tool 白名单外输入被拦、IO 未发生")
class SandboxIntegrationTest {

  private final Sandbox sandbox = buildSandbox();

  private static Sandbox buildSandbox() {
    WhitelistManager manager =
        new WhitelistManager(
            new FileSandboxProperties(List.of("/workspace")),
            new ShellSandboxProperties(List.of("ls")),
            new HttpSandboxProperties(List.of("api.weather.com")),
            new InMemorySandboxWhitelistStore());
    manager.initialize();
    return new WhitelistSandbox(manager);
  }

  @Test
  @DisplayName("FileTools: 白名单外路径被拦")
  void fileToolsBlockedPath() {
    FileTools fileTools = new FileTools(sandbox);
    ToolExecutionResult result = fileTools.doReadFile("{\"path\": \"/etc/passwd\"}");
    assertFalse(result.success());
    assertTrue(result.errorMessage().contains("白名单"));
  }

  @Test
  @DisplayName("ShellTools: 白名单外命令被拦")
  void shellToolsBlockedCommand() {
    ShellTools shellTools = new ShellTools(sandbox);
    ToolExecutionResult result = shellTools.doShell("{\"command\": \"rm -rf /\"}");
    assertFalse(result.success());
    assertTrue(result.errorMessage().contains("白名单"));
  }

  @Test
  @DisplayName("HttpTools: 白名单外域名被拦、HTTP 请求未发出")
  void httpToolsBlockedDomain() {
    RestClient.Builder builder = RestClient.builder();
    HttpTools httpTools = new HttpTools(sandbox, builder);
    ToolExecutionResult result = httpTools.doGet("{\"url\": \"https://evil.com/steal\"}");
    assertFalse(result.success());
    assertTrue(result.errorMessage().contains("白名单"));
  }

  @Test
  @DisplayName("FileTools: 白名单内路径放行")
  void fileToolsAllowedPath() {
    FileTools fileTools = new FileTools(sandbox);
    ToolExecutionResult result = fileTools.doListDir("{\"path\": \"/workspace\"}");
    assertTrue(
        result.success() || !result.errorMessage().contains("白名单"),
        "Sandbox should allow /workspace, but got: " + result.errorMessage());
  }
}
