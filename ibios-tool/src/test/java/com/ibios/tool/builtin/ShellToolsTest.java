package com.ibios.tool.builtin;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ibios.core.tool.Sandbox;
import com.ibios.core.tool.SandboxAction;
import com.ibios.core.tool.SandboxViolationException;
import com.ibios.core.tool.ToolExecutionResult;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ShellToolsTest {

  private ShellTools shellTools;
  private TestSandbox sandbox;

  @BeforeEach
  void setUp() {
    sandbox = new TestSandbox();
    shellTools = new ShellTools(sandbox);
  }

  @Test
  void shellShouldExecuteAllowedCommand() {
    sandbox.allowCommand("echo");

    ToolExecutionResult result = shellTools.doShell("{\"command\":\"echo\",\"args\":[\"hello\"]}");

    assertTrue(result.success());
    assertTrue(result.output().contains("hello"));
  }

  @Test
  void shellOutsideAllowedCommandShouldFail() {
    // Don't allow the command in sandbox

    ToolExecutionResult result =
        shellTools.doShell("{\"command\":\"rm\",\"args\":[\"-rf\",\"/\"]}");

    assertFalse(result.success());
    assertTrue(
        result.errorMessage().contains("not allowed") || result.errorMessage().contains("Sandbox"));
  }

  private static class TestSandbox implements Sandbox {
    private final Set<String> allowedCommands = new HashSet<>();

    void allowCommand(String command) {
      allowedCommands.add(command);
    }

    @Override
    public void enforce(SandboxAction action) {
      if (!allowedCommands.contains(action.target())) {
        throw new SandboxViolationException("Command not allowed: " + action.target());
      }
    }
  }
}
