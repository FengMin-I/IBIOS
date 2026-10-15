package com.ibios.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ibios.core.memory.LongTermMemoryStore;
import com.ibios.core.memory.MemoryScope;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:sqlite:file:contractdb?mode=memory&cache=shared",
      "spring.datasource.driver-class-name=org.sqlite.JDBC",
      "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
      "spring.jpa.hibernate.ddl-auto=create-drop"
    })
@DisplayName("契约测试：三档 LongTermMemoryStore 实现统一验证")
class MemoryStoreContractTest {

  @TempDir Path tempDir;

  @Autowired private MemoryEntryRepository repository;

  private MarkdownMemoryStore markdownStore;
  private SqliteMemoryStore sqliteStore;
  private InMemoryMem0Stub mem0Stub;

  @BeforeEach
  void setUp() {
    markdownStore = new MarkdownMemoryStore(tempDir.resolve("MEMORY-" + System.nanoTime() + ".md"));
    sqliteStore = new SqliteMemoryStore(repository);
    mem0Stub = new InMemoryMem0Stub();
    repository.deleteAll();
    mem0Stub.clear();
  }

  @Test
  @DisplayName("契约一：写后立读——不缓存（三档实现）")
  void writeThenReadImmediately() {
    for (LongTermMemoryStore store : allStores()) {
      store.append("刚记的事", MemoryScope.ARCHIVAL);
      String loaded = store.load();
      assertTrue(loaded.contains("刚记的事"), "Failed for " + store.getClass().getSimpleName());
    }
  }

  @Test
  @DisplayName("契约二：截断只裁归档——核心记忆一字不动（三档实现）")
  void truncationOnlyAffectsArchive() {
    for (LongTermMemoryStore store : allStores()) {
      store.append("核心不能丢", MemoryScope.CORE);
      StringBuilder sb = new StringBuilder();
      for (int i = 0; i < 500; i++) {
        sb.append("归档流水行").append(i).append(" ");
      }
      store.append(sb.toString(), MemoryScope.ARCHIVAL);
      String loaded = store.load();
      assertTrue(loaded.contains("核心不能丢"), "Failed for " + store.getClass().getSimpleName());
    }
  }

  @Test
  @DisplayName("契约三：scope 路由——CORE 和 ARCHIVAL 分别存储（三档实现）")
  void scopeRouting() {
    for (LongTermMemoryStore store : allStores()) {
      store.append("核心内容", MemoryScope.CORE);
      store.append("归档内容", MemoryScope.ARCHIVAL);
      String loaded = store.load();
      assertTrue(loaded.contains("核心内容"), "Core missing for " + store.getClass().getSimpleName());
      assertTrue(
          loaded.contains("归档内容"), "Archive missing for " + store.getClass().getSimpleName());
    }
  }

  @Test
  @DisplayName("契约四：recall 只搜归档区（三档实现）")
  void recallOnlySearchesArchive() {
    for (LongTermMemoryStore store : allStores()) {
      store.append("关键词ABC核心", MemoryScope.CORE);
      store.append("关键词ABC归档", MemoryScope.ARCHIVAL);
      List<String> hits = store.recallByKeyword("关键词ABC");
      assertEquals(1, hits.size(), "Expected 1 hit for " + store.getClass().getSimpleName());
      assertTrue(
          hits.get(0).contains("归档"),
          "Hit should be from archive for " + store.getClass().getSimpleName());
      assertFalse(
          hits.get(0).contains("核心"),
          "Hit should not be from core for " + store.getClass().getSimpleName());
    }
  }

  private LongTermMemoryStore[] allStores() {
    return new LongTermMemoryStore[] {markdownStore, sqliteStore, mem0Stub};
  }
}
