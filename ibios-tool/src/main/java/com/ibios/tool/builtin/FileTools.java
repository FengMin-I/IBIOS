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
import java.util.List;
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

  @Component("editFileTool")
  static final class EditFileTool implements IbiosTool {

    private final FileTools delegate;

    EditFileTool(FileTools delegate) {
      this.delegate = delegate;
    }

    @Override
    public String name() {
      return "edit_file";
    }

    @Override
    public String description() {
      return "Replace a unique occurrence of old_text with new_text in a file. old_text must appear exactly once.";
    }

    @Override
    public String inputSchema() {
      return """
          {
            "type": "object",
            "properties": {
              "file_path": {"type": "string", "description": "Path to the file to edit"},
              "old_text": {"type": "string", "description": "Text to replace (must appear exactly once)"},
              "new_text": {"type": "string", "description": "Replacement text"}
            },
            "required": ["file_path", "old_text", "new_text"]
          }
          """;
    }

    @Override
    public ToolExecutionResult execute(String argumentsJson) {
      return delegate.doEditFile(argumentsJson);
    }
  }

  ToolExecutionResult doEditFile(String argumentsJson) {
    try {
      JsonNode args = objectMapper.readTree(argumentsJson);
      String filePath = args.get("file_path").asText();
      String oldText = args.get("old_text").asText();
      String newText = args.get("new_text").asText();

      sandbox.enforce(new SandboxAction(ActionType.FILE_WRITE, filePath));

      Path path = Path.of(filePath);
      String content = Files.readString(path);

      // Count occurrences
      int count = 0;
      int index = 0;
      while ((index = content.indexOf(oldText, index)) != -1) {
        count++;
        index += oldText.length();
      }

      if (count == 0) {
        return ToolExecutionResult.failure("Text not found in file", false);
      }
      if (count > 1) {
        // Find line numbers
        StringBuilder lineNumbers = new StringBuilder();
        String[] lines = content.split("\n", -1);
        for (int i = 0; i < lines.length; i++) {
          if (lines[i].contains(oldText)) {
            if (lineNumbers.length() > 0) {
              lineNumbers.append(", ");
            }
            lineNumbers.append(i + 1);
          }
        }
        return ToolExecutionResult.failure(
            String.format(
                "Text appears %d times at lines [%s]. Provide more context.", count, lineNumbers),
            false);
      }

      // Exactly one occurrence - replace
      String newContent = content.replace(oldText, newText);
      Files.writeString(path, newContent);
      log.info("edit_file: {} (1 replacement)", filePath);
      return ToolExecutionResult.success("");
    } catch (Exception e) {
      return ToolExecutionResult.failure("Failed to edit file: " + e.getMessage(), false);
    }
  }

  @Component("grepTool")
  static final class GrepTool implements IbiosTool {

    private final FileTools delegate;

    GrepTool(FileTools delegate) {
      this.delegate = delegate;
    }

    @Override
    public String name() {
      return "grep";
    }

    @Override
    public String description() {
      return "Search for a regex pattern in files under a path, returns file:line_number:content";
    }

    @Override
    public String inputSchema() {
      return """
          {
            "type": "object",
            "properties": {
              "pattern": {"type": "string", "description": "Regex pattern to search for"},
              "path": {"type": "string", "description": "File or directory to search in"},
              "case_sensitive": {"type": "boolean", "default": true, "description": "Case-sensitive search"}
            },
            "required": ["pattern", "path"]
          }
          """;
    }

    @Override
    public ToolExecutionResult execute(String argumentsJson) {
      return delegate.doGrep(argumentsJson);
    }
  }

  ToolExecutionResult doGrep(String argumentsJson) {
    try {
      JsonNode args = objectMapper.readTree(argumentsJson);
      String pattern = args.get("pattern").asText();
      String path = args.get("path").asText();
      boolean caseSensitive =
          args.has("case_sensitive") ? args.get("case_sensitive").asBoolean() : true;

      sandbox.enforce(new SandboxAction(ActionType.FILE_READ, path));

      int flags = caseSensitive ? 0 : java.util.regex.Pattern.CASE_INSENSITIVE;
      java.util.regex.Pattern regex = java.util.regex.Pattern.compile(pattern, flags);

      StringBuilder results = new StringBuilder();
      int[] count = {0};
      boolean[] truncated = {false};
      final int LIMIT = 200;

      Files.walk(Path.of(path))
          .filter(Files::isRegularFile)
          .forEach(
              file -> {
                if (count[0] >= LIMIT) {
                  truncated[0] = true;
                  return;
                }
                try {
                  List<String> lines = Files.readAllLines(file);
                  for (int i = 0; i < lines.size(); i++) {
                    if (count[0] >= LIMIT) {
                      truncated[0] = true;
                      break;
                    }
                    if (regex.matcher(lines.get(i)).find()) {
                      results
                          .append(file)
                          .append(":")
                          .append(i + 1)
                          .append(":")
                          .append(lines.get(i))
                          .append("\n");
                      count[0]++;
                    }
                  }
                } catch (Exception e) {
                  // Skip binary/non-UTF-8 files
                  log.warn("grep: skipping file {} ({})", file, e.getMessage());
                }
              });

      if (truncated[0]) {
        results.append("... (results truncated at ").append(LIMIT).append(")\n");
      }

      log.info("grep: pattern='{}' path={} matches={}", pattern, path, count[0]);
      return ToolExecutionResult.success(results.toString());
    } catch (Exception e) {
      return ToolExecutionResult.failure("Failed to grep: " + e.getMessage(), false);
    }
  }

  @Component("globTool")
  static final class GlobTool implements IbiosTool {

    private final FileTools delegate;

    GlobTool(FileTools delegate) {
      this.delegate = delegate;
    }

    @Override
    public String name() {
      return "glob";
    }

    @Override
    public String description() {
      return "Find files by glob pattern (e.g., **/*.yaml)";
    }

    @Override
    public String inputSchema() {
      return """
          {
            "type": "object",
            "properties": {
              "pattern": {"type": "string", "description": "Glob pattern (e.g., **/*.yaml)"},
              "path": {"type": "string", "description": "Root directory to search from"}
            },
            "required": ["pattern", "path"]
          }
          """;
    }

    @Override
    public ToolExecutionResult execute(String argumentsJson) {
      return delegate.doGlob(argumentsJson);
    }
  }

  ToolExecutionResult doGlob(String argumentsJson) {
    try {
      JsonNode args = objectMapper.readTree(argumentsJson);
      String pattern = args.get("pattern").asText();
      String path = args.get("path").asText();

      sandbox.enforce(new SandboxAction(ActionType.FILE_READ, path));

      java.nio.file.PathMatcher matcher =
          java.nio.file.FileSystems.getDefault().getPathMatcher("glob:" + pattern);

      StringBuilder results = new StringBuilder();
      int[] count = {0};
      boolean[] truncated = {false};
      final int LIMIT = 200;

      Files.walk(Path.of(path))
          .filter(matcher::matches)
          .filter(Files::isRegularFile)
          .forEach(
              file -> {
                if (count[0] >= LIMIT) {
                  truncated[0] = true;
                  return;
                }
                results.append(file).append("\n");
                count[0]++;
              });

      if (truncated[0]) {
        results.append("... (results truncated at ").append(LIMIT).append(")\n");
      }

      log.info("glob: pattern='{}' path={} matches={}", pattern, path, count[0]);
      return ToolExecutionResult.success(results.toString());
    } catch (Exception e) {
      return ToolExecutionResult.failure("Failed to glob: " + e.getMessage(), false);
    }
  }
}
