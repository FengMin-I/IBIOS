package com.ibios.tool.builtin;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ibios.core.tool.Sandbox;
import com.ibios.core.tool.SandboxAction;
import com.ibios.core.tool.SandboxViolationException;
import com.ibios.core.tool.ToolExecutionResult;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileToolsTest {

  @TempDir Path tempDir;

  private FileTools fileTools;
  private TestSandbox sandbox;

  @BeforeEach
  void setUp() {
    sandbox = new TestSandbox();
    fileTools = new FileTools(sandbox);
  }

  @Test
  void readFileShouldReturnContent() throws IOException {
    Path file = tempDir.resolve("test.txt");
    Files.writeString(file, "hello world");
    sandbox.allowPath(file.toString());

    ToolExecutionResult result = fileTools.doReadFile("{\"path\":\"" + file + "\"}");

    assertTrue(result.success());
    assertTrue(result.output().contains("hello world"));
  }

  @Test
  void writeFileShouldCreateFile() {
    Path file = tempDir.resolve("output.txt");
    sandbox.allowPath(file.toString());

    ToolExecutionResult result =
        fileTools.doWriteFile("{\"path\":\"" + file + "\",\"content\":\"new content\"}");

    assertTrue(result.success());
    assertTrue(Files.exists(file));
  }

  @Test
  void listDirShouldReturnEntries() throws IOException {
    Files.createFile(tempDir.resolve("a.txt"));
    Files.createDirectory(tempDir.resolve("subdir"));
    sandbox.allowPath(tempDir.toString());

    ToolExecutionResult result = fileTools.doListDir("{\"path\":\"" + tempDir + "\"}");

    assertTrue(result.success());
    assertTrue(result.output().contains("a.txt"));
    assertTrue(result.output().contains("subdir/"));
  }

  @Test
  void readFileOutsideAllowedPathShouldFail() throws IOException {
    Path file = tempDir.resolve("secret.txt");
    Files.writeString(file, "secret");
    // Don't allow the path in sandbox

    ToolExecutionResult result = fileTools.doReadFile("{\"path\":\"" + file + "\"}");

    assertFalse(result.success());
    assertTrue(
        result.errorMessage().contains("not allowed") || result.errorMessage().contains("Sandbox"));
  }

  private static class TestSandbox implements Sandbox {
    private final java.util.Set<String> allowedPaths = new java.util.HashSet<>();

    void allowPath(String path) {
      allowedPaths.add(path);
    }

    @Override
    public void enforce(SandboxAction action) {
      String target = action.target();
      boolean allowed = allowedPaths.stream().anyMatch(target::startsWith);
      if (!allowed) {
        throw new SandboxViolationException("Path not allowed: " + target);
      }
    }
  }
}
