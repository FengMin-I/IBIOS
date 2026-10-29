package com.ibios.memory;

import com.ibios.core.memory.LongTermMemoryStore;
import com.ibios.core.memory.MemoryScope;
import com.ibios.core.workspace.WorkspaceRoot;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.List;

public class MarkdownMemoryStore implements LongTermMemoryStore {

  private static final String CORE_HEADER = "## 核心记忆";
  private static final String ARCHIVE_HEADER = "## 归档记忆";
  private static final int MAX_ARCHIVE_CHARS = 4000;
  private static final DateTimeFormatter TIMESTAMP_FORMAT =
      DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

  private final Path memoryFilePath;

  public MarkdownMemoryStore() {
    this(WorkspaceRoot.resolve().memoryFile());
  }

  public MarkdownMemoryStore(Path memoryFilePath) {
    this.memoryFilePath = memoryFilePath;
  }

  @Override
  public void append(String content, MemoryScope scope) {
    String header = scope == MemoryScope.CORE ? CORE_HEADER : ARCHIVE_HEADER;
    String entry = "\n- [" + LocalDateTime.now().format(TIMESTAMP_FORMAT) + "] " + content;
    writeIntoSection(header, entry);
  }

  @Override
  public String load() {
    String raw = readFile();
    String core = extractSection(raw, CORE_HEADER);
    String archive = truncateIfNeeded(extractSection(raw, ARCHIVE_HEADER));
    return core + "\n" + archive;
  }

  @Override
  public List<String> recallByKeyword(String keyword) {
    String raw = readFile();
    String archive = extractSection(raw, ARCHIVE_HEADER);
    if (archive.isBlank()) {
      return Collections.emptyList();
    }
    return archive.lines().filter(line -> line.contains(keyword)).toList();
  }

  Path getMemoryFilePath() {
    return memoryFilePath;
  }

  private String readFile() {
    if (!Files.exists(memoryFilePath)) {
      return "";
    }
    try {
      return Files.readString(memoryFilePath);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read memory file: " + memoryFilePath, e);
    }
  }

  private void writeIntoSection(String header, String entry) {
    String raw = readFile();
    if (raw.isEmpty()) {
      raw = CORE_HEADER + "\n" + ARCHIVE_HEADER + "\n";
    }
    int sectionStart = raw.indexOf(header);
    if (sectionStart < 0) {
      raw = raw + "\n" + header + "\n";
      sectionStart = raw.indexOf(header);
    }
    int nextSection = findNextHeader(raw, sectionStart + header.length());
    String insertPoint;
    if (nextSection >= 0) {
      insertPoint = raw.substring(0, nextSection) + entry + "\n" + raw.substring(nextSection);
    } else {
      insertPoint = raw + entry + "\n";
    }
    writeFile(insertPoint);
  }

  private int findNextHeader(String raw, int fromIndex) {
    int coreIdx = raw.indexOf(CORE_HEADER, fromIndex);
    int archiveIdx = raw.indexOf(ARCHIVE_HEADER, fromIndex);
    if (coreIdx < 0 && archiveIdx < 0) {
      return -1;
    }
    if (coreIdx < 0) {
      return archiveIdx;
    }
    if (archiveIdx < 0) {
      return coreIdx;
    }
    return Math.min(coreIdx, archiveIdx);
  }

  String extractSection(String raw, String header) {
    int headerIdx = raw.indexOf(header);
    if (headerIdx < 0) {
      return "";
    }
    int contentStart = raw.indexOf('\n', headerIdx);
    if (contentStart < 0) {
      return "";
    }
    contentStart = contentStart + 1;
    int nextHeader = findNextHeader(raw, contentStart);
    if (nextHeader >= 0) {
      return raw.substring(contentStart, nextHeader).strip();
    }
    return raw.substring(contentStart).strip();
  }

  private String truncateIfNeeded(String archive) {
    if (archive.length() <= MAX_ARCHIVE_CHARS) {
      return archive;
    }
    return archive.substring(archive.length() - MAX_ARCHIVE_CHARS);
  }

  private void writeFile(String content) {
    try {
      Files.createDirectories(memoryFilePath.getParent());
      Files.writeString(memoryFilePath, content);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to write memory file: " + memoryFilePath, e);
    }
  }
}
