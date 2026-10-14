package com.ibios.tool.builtin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibios.core.tool.IbiosTool;
import com.ibios.core.tool.Sandbox;
import com.ibios.core.tool.SandboxAction;
import com.ibios.core.tool.SandboxAction.ActionType;
import com.ibios.core.tool.ToolExecutionResult;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ShellTools {

  private static final Logger log = LoggerFactory.getLogger(ShellTools.class);
  private static final int DEFAULT_TIMEOUT_SECONDS = 30;

  private final Sandbox sandbox;
  private final ObjectMapper objectMapper;

  public ShellTools(Sandbox sandbox) {
    this.sandbox = sandbox;
    this.objectMapper = new ObjectMapper();
  }

  @Component("shellTool")
  static final class ShellTool implements IbiosTool {

    private final ShellTools delegate;

    ShellTool(ShellTools delegate) {
      this.delegate = delegate;
    }

    @Override
    public String name() {
      return "shell";
    }

    @Override
    public String description() {
      return "Execute a shell command with arguments and return stdout";
    }

    @Override
    public String inputSchema() {
      return """
          {"type":"object","properties":{"command":{"type":"string","description":"Command to exec\
          ute"},"args":{"type":"array","items":{"type":"string"},"description":"Command arguments"}\
          ,"timeout_seconds":{"type":"integer","description":"Timeout in seconds (default 30)"}},"r\
          equired":["command"]}""";
    }

    @Override
    public ToolExecutionResult execute(String argumentsJson) {
      return delegate.doShell(argumentsJson);
    }
  }

  ToolExecutionResult doShell(String argumentsJson) {
    try {
      JsonNode args = objectMapper.readTree(argumentsJson);
      String command = args.get("command").asText();
      sandbox.enforce(new SandboxAction(ActionType.SHELL_COMMAND, command));

      List<String> cmdList = new ArrayList<>();
      cmdList.add(command);
      if (args.has("args") && args.get("args").isArray()) {
        for (JsonNode arg : args.get("args")) {
          cmdList.add(arg.asText());
        }
      }

      int timeout =
          args.has("timeout_seconds")
              ? args.get("timeout_seconds").asInt()
              : DEFAULT_TIMEOUT_SECONDS;

      ProcessBuilder pb = new ProcessBuilder(cmdList);
      pb.redirectErrorStream(true);
      Process process = pb.start();

      StringBuilder output = new StringBuilder();
      try (BufferedReader reader =
          new BufferedReader(new InputStreamReader(process.getInputStream()))) {
        String line;
        while ((line = reader.readLine()) != null) {
          output.append(line).append("\n");
        }
      }

      boolean finished = process.waitFor(timeout, TimeUnit.SECONDS);
      if (!finished) {
        process.destroyForcibly();
        return ToolExecutionResult.failure("Command timed out after " + timeout + " seconds", true);
      }

      log.info("shell: {} (exit={})", command, process.exitValue());
      return ToolExecutionResult.success(output.toString());
    } catch (Exception e) {
      return ToolExecutionResult.failure("Shell execution failed: " + e.getMessage(), false);
    }
  }
}
