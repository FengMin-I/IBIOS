package com.ibios.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ibios.core.memory.MemoryScope;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MarkdownMemoryStoreTest {

  @TempDir Path tempDir;

  private MarkdownMemoryStore store;

  @BeforeEach
  void setUp() {
    store = new MarkdownMemoryStore(tempDir.resolve("MEMORY.md"));
  }

  @Test
  @DisplayName("文件不存在时自动创建目录和文件")
  void autoCreateFileWhenNotExists() {
    store.append("test content", MemoryScope.ARCHIVAL);
    assertTrue(Files.exists(store.getMemoryFilePath()));
  }

  @Test
  @DisplayName("核心记忆写入核心区块")
  void coreMemoryWritesToCoreSection() {
    store.append("用户叫小王", MemoryScope.CORE);
    String loaded = store.load();
    assertTrue(loaded.contains("用户叫小王"));
  }

  @Test
  @DisplayName("归档记忆写入归档区块")
  void archivalMemoryWritesToArchiveSection() {
    store.append("今天天气好", MemoryScope.ARCHIVAL);
    String loaded = store.load();
    assertTrue(loaded.contains("今天天气好"));
  }

  @Test
  @DisplayName("区块 header 解析正确——核心和归档互不干扰")
  void sectionHeaderParsingCorrect() throws Exception {
    store.append("核心内容", MemoryScope.CORE);
    store.append("归档内容", MemoryScope.ARCHIVAL);
    String raw = Files.readString(store.getMemoryFilePath());
    String coreSection = store.extractSection(raw, "## 核心记忆");
    String archiveSection = store.extractSection(raw, "## 归档记忆");
    assertTrue(coreSection.contains("核心内容"));
    assertFalse(coreSection.contains("归档内容"));
    assertTrue(archiveSection.contains("归档内容"));
    assertFalse(archiveSection.contains("核心内容"));
  }

  @Test
  @DisplayName("归档区截断——超过 4000 字符时只裁归档段")
  void archiveTruncationAt4000Chars() {
    store.append("核心不能丢", MemoryScope.CORE);
    StringBuilder sb = new StringBuilder();
    for (int i = 0; i < 500; i++) {
      String line = "归档流水行" + i + " ";
      sb.append(line);
    }
    store.append(sb.toString(), MemoryScope.ARCHIVAL);
    String loaded = store.load();
    assertTrue(loaded.contains("核心不能丢"));
  }

  @Test
  @DisplayName("recall 只搜归档区——核心记忆不参与检索")
  void recallOnlySearchesArchive() {
    store.append("核心关键词ABC", MemoryScope.CORE);
    store.append("归档关键词ABC", MemoryScope.ARCHIVAL);
    List<String> hits = store.recallByKeyword("关键词ABC");
    assertEquals(1, hits.size());
    assertTrue(hits.get(0).contains("归档关键词ABC"));
  }

  @Test
  @DisplayName("recall 未命中返回空列表")
  void recallNoMatchReturnsEmpty() {
    store.append("某条记忆", MemoryScope.ARCHIVAL);
    List<String> hits = store.recallByKeyword("不存在的词");
    assertTrue(hits.isEmpty());
  }

  @Test
  @DisplayName("写入后立即可读——不缓存")
  void writeThenReadImmediately() {
    store.append("刚记的事", MemoryScope.ARCHIVAL);
    String loaded = store.load();
    assertTrue(loaded.contains("刚记的事"));
    assertFalse(store.recallByKeyword("刚记的事").isEmpty());
  }
}
