package com.ibios.tool.builtin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibios.core.tool.IbiosTool;
import com.ibios.core.tool.Sandbox;
import com.ibios.core.tool.SandboxAction;
import com.ibios.core.tool.SandboxAction.ActionType;
import com.ibios.core.tool.ToolExecutionResult;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class FileTools {

  private static final Logger log = LoggerFactory.getLogger(FileTools.class);

  private final Sandbox sandbox;
  private final ObjectMapper objectMapper;

  public FileTools(Sandbox sandbox) {
    this.sandbox = sandbox;
    this.objectMapper = new ObjectMapper();
  }

  @Component("readFileTool")
  static final class ReadFileTool implements IbiosTool {

    private final FileTools delegate;

    ReadFileTool(FileTools delegate) {
      this.delegate = delegate;
    }

    @Override
    public String name() {
      return "read_file";
    }

    @Override
    public String description() {
      return "Read the contents of a file at the given path";
    }

    @Override
    public String inputSchema() {
      return """
          {"type":"object","properties":{"path":{"type":"string","description":"File path to read"}}\
          ,"required":["path"]}""";
    }

    @Override
    public ToolExecutionResult execute(String argumentsJson) {
      return delegate.doReadFile(argumentsJson);
    }
  }

  @Component("writeFileTool")
  static final class WriteFileTool implements IbiosTool {

    private final FileTools delegate;

    WriteFileTool(FileTools delegate) {
      this.delegate = delegate;
    }

    @Override
    public String name() {
      return "write_file";
    }

    @Override
    public String description() {
      return "Write content to a file at the given path, creating parent directories if needed";
    }

    @Override
    public String inputSchema() {
      return """
          {"type":"object","properties":{"path":{"type":"string","description":"File path to write"\
          },"content":{"type":"string","description":"Content to write"}},"required":["path","cont\
          ent"]}""";
    }

    @Override
    public ToolExecutionResult execute(String argumentsJson) {
      return delegate.doWriteFile(argumentsJson);
    }
  }

  @Component("listDirTool")
  static final class ListDirTool implements IbiosTool {

    private final FileTools delegate;

    ListDirTool(FileTools delegate) {
      this.delegate = delegate;
    }

    @Override
    public String name() {
      return "list_dir";
    }

    @Override
    public String description() {
      return "List files and directories at the given path";
    }

    @Override
    public String inputSchema() {
      return """
          {"type":"object","properties":{"path":{"type":"string","description":"Directory path to l\
          ist"}},"required":["path"]}""";
    }

    @Override
    public ToolExecutionResult execute(String argumentsJson) {
      return delegate.doListDir(argumentsJson);
    }
  }

  ToolExecutionResult doReadFile(String argumentsJson) {
    try {
      JsonNode args = objectMapper.readTree(argumentsJson);
      String path = args.get("path").asText();
      sandbox.enforce(new SandboxAction(ActionType.FILE_READ, path));
      String content = Files.readString(Path.of(path));
      log.info("read_file: {}", path);
      return ToolExecutionResult.success(content);
    } catch (Exception e) {
      return ToolExecutionResult.failure("Failed to read file: " + e.getMessage(), false);
    }
  }

  ToolExecutionResult doWriteFile(String argumentsJson) {
    try {
      JsonNode args = objectMapper.readTree(argumentsJson);
      String path = args.get("path").asText();
      String content = args.get("content").asText();
      sandbox.enforce(new SandboxAction(ActionType.FILE_WRITE, path));
      Path filePath = Path.of(path);
      Files.createDirectories(filePath.getParent());
      Files.writeString(filePath, content);
      log.info("write_file: {}", path);
      return ToolExecutionResult.success("File written successfully");
    } catch (Exception e) {
      return ToolExecutionResult.failure("Failed to write file: " + e.getMessage(), false);
    }
  }

  ToolExecutionResult doListDir(String argumentsJson) {
    try {
      JsonNode args = objectMapper.readTree(argumentsJson);
      String path = args.get("path").asText();
      sandbox.enforce(new SandboxAction(ActionType.FILE_READ, path));
      StringBuilder sb = new StringBuilder();
      try (DirectoryStream<Path> stream = Files.newDirectoryStream(Path.of(path))) {
        for (Path entry : stream) {
          sb.append(entry.getFileName()).append(Files.isDirectory(entry) ? "/" : "").append("\n");
        }
      }
      log.info("list_dir: {}", path);
      return ToolExecutionResult.success(sb.toString());
    } catch (Exception e) {
      return ToolExecutionResult.failure("Failed to list directory: " + e.getMessage(), false);
    }
  }
}
