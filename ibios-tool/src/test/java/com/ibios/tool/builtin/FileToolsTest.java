package com.ibios.tool.builtin;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
import org.junit.jupiter.api.DisplayName;
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

  // --- edit_file tests ---

  @Test
  @DisplayName("edit_file: 唯一匹配成功替换")
  void editFileUniqueMatchSuccess() throws IOException {
    Path file = tempDir.resolve("edit.txt");
    Files.writeString(file, "hello world\nfoo bar\n");
    sandbox.allowPath(file.toString());

    String args =
        "{\"file_path\":\"" + file + "\",\"old_text\":\"foo bar\",\"new_text\":\"baz qux\"}";
    ToolExecutionResult result = fileTools.doEditFile(args);

    assertTrue(result.success());
    String content = Files.readString(file);
    assertTrue(content.contains("baz qux"));
    assertFalse(content.contains("foo bar"));
  }

  @Test
  @DisplayName("edit_file: 旧文本找不到报错")
  void editFileNotFoundShouldError() throws IOException {
    Path file = tempDir.resolve("edit2.txt");
    Files.writeString(file, "hello world\n");
    sandbox.allowPath(file.toString());
    String original = Files.readString(file);

    String args =
        "{\"file_path\":\"" + file + "\",\"old_text\":\"missing\",\"new_text\":\"replacement\"}";
    ToolExecutionResult result = fileTools.doEditFile(args);

    assertFalse(result.success());
    assertTrue(result.errorMessage().contains("not found"));
    assertEquals(original, Files.readString(file));
  }

  @Test
  @DisplayName("edit_file: 旧文本出现多次报错")
  void editFileMultipleMatchesShouldError() throws IOException {
    Path file = tempDir.resolve("edit3.txt");
    Files.writeString(file, "aaa\nbbb\naaa\n");
    sandbox.allowPath(file.toString());
    String original = Files.readString(file);

    String args = "{\"file_path\":\"" + file + "\",\"old_text\":\"aaa\",\"new_text\":\"ccc\"}";
    ToolExecutionResult result = fileTools.doEditFile(args);

    assertFalse(result.success());
    assertTrue(result.errorMessage().contains("2 times"));
    assertEquals(original, Files.readString(file));
  }

  // --- grep tests ---

  @Test
  @DisplayName("grep: 正常搜索返回格式正确")
  void grepNormalSearchReturnsResults() throws IOException {
    Path file = tempDir.resolve("search.txt");
    Files.writeString(file, "line one\nline two has target\nline three\n");
    sandbox.allowPath(tempDir.toString());

    String args = "{\"pattern\":\"target\",\"path\":\"" + tempDir + "\"}";
    ToolExecutionResult result = fileTools.doGrep(args);

    assertTrue(result.success());
    assertTrue(result.output().contains("search.txt:2:"));
    assertTrue(result.output().contains("target"));
  }

  @Test
  @DisplayName("grep: 超 200 条截断")
  void grepOverLimitTruncates() throws IOException {
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 250; i++) {
      sb.append("match line ").append(i).append("\n");
    }
    Path file = tempDir.resolve("big.txt");
    Files.writeString(file, sb.toString());
    sandbox.allowPath(tempDir.toString());

    String args = "{\"pattern\":\"match\",\"path\":\"" + tempDir + "\"}";
    ToolExecutionResult result = fileTools.doGrep(args);

    assertTrue(result.success());
    assertTrue(result.output().contains("truncated"));
  }

  @Test
  @DisplayName("grep: 二进制文件跳过")
  void grepBinaryFileSkips() throws IOException {
    Path textFile = tempDir.resolve("text.txt");
    Files.writeString(textFile, "hello target");
    Path binFile = tempDir.resolve("binary.bin");
    Files.write(binFile, new byte[] {0x00, 0x01, 0x02, (byte) 0xFF, (byte) 0xFE});
    sandbox.allowPath(tempDir.toString());

    String args = "{\"pattern\":\"target\",\"path\":\"" + tempDir + "\"}";
    ToolExecutionResult result = fileTools.doGrep(args);

    assertTrue(result.success());
    assertTrue(result.output().contains("text.txt"));
  }

  // --- glob tests ---

  @Test
  @DisplayName("glob: 通配符匹配正确")
  void globPatternMatchReturnsFiles() throws IOException {
    Files.createFile(tempDir.resolve("a.yaml"));
    Files.createFile(tempDir.resolve("b.yaml"));
    Files.createFile(tempDir.resolve("c.txt"));
    sandbox.allowPath(tempDir.toString());

    String args = "{\"pattern\":\"**/*.yaml\",\"path\":\"" + tempDir + "\"}";
    ToolExecutionResult result = fileTools.doGlob(args);

    assertTrue(result.success());
    assertTrue(result.output().contains("a.yaml"));
    assertTrue(result.output().contains("b.yaml"));
    assertFalse(result.output().contains("c.txt"));
  }

  @Test
  @DisplayName("glob: 超 200 条截断")
  void globOverLimitTruncates() throws IOException {
    for (int i = 0; i < 250; i++) {
      Files.createFile(tempDir.resolve("file" + i + ".txt"));
    }
    sandbox.allowPath(tempDir.toString());

    String args = "{\"pattern\":\"**/*.txt\",\"path\":\"" + tempDir + "\"}";
    ToolExecutionResult result = fileTools.doGlob(args);

    assertTrue(result.success());
    assertTrue(result.output().contains("truncated"));
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
