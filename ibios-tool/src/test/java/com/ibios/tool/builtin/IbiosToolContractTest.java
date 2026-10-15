package com.ibios.tool.builtin;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.ibios.core.tool.IbiosTool;
import com.ibios.core.tool.Sandbox;
import com.ibios.tool.sandbox.FileSandboxProperties;
import com.ibios.tool.sandbox.HttpSandboxProperties;
import com.ibios.tool.sandbox.ShellSandboxProperties;
import com.ibios.tool.sandbox.WhitelistSandbox;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.web.client.RestClient;

class IbiosToolContractTest {

  static Stream<IbiosTool> allRegisteredTools() {
    Sandbox sandbox =
        new WhitelistSandbox(
            new FileSandboxProperties(List.of("/")),
            new ShellSandboxProperties(List.of("ls", "cat", "echo", "sh")),
            new HttpSandboxProperties(List.of("*")));
    FileTools fileTools = new FileTools(sandbox);
    ShellTools shellTools = new ShellTools(sandbox);
    HttpTools httpTools = new HttpTools(sandbox, RestClient.builder());

    return Stream.of(
        new FileTools.ReadFileTool(fileTools),
        new FileTools.WriteFileTool(fileTools),
        new FileTools.ListDirTool(fileTools),
        new ShellTools.ShellTool(shellTools),
        new HttpTools.HttpGetTool(httpTools),
        new HttpTools.HttpPostTool(httpTools));
  }

  @ParameterizedTest
  @MethodSource("allRegisteredTools")
  void eachToolContractTripleMustBeNonNull(IbiosTool tool) {
    assertNotNull(tool.name(), "Tool name must not be null");
    assertNotNull(tool.description(), "Tool description must not be null");
    assertNotNull(tool.inputSchema(), "Tool inputSchema must not be null");
  }
}
