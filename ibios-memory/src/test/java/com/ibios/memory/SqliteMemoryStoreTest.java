package com.ibios.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ibios.core.memory.MemoryScope;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:sqlite:file:memdb?mode=memory&cache=shared",
      "spring.datasource.driver-class-name=org.sqlite.JDBC",
      "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
      "spring.jpa.hibernate.ddl-auto=create-drop"
    })
class SqliteMemoryStoreTest {

  @Autowired private MemoryEntryRepository repository;

  private SqliteMemoryStore store;

  @BeforeEach
  void setUp() {
    repository.deleteAll();
    store = new SqliteMemoryStore(repository);
  }

  @Test
  @DisplayName("建表能建能读——写入后立即读回")
  void writeThenReadImmediately() {
    store.append("测试记忆", MemoryScope.ARCHIVAL);
    String loaded = store.load();
    assertTrue(loaded.contains("测试记忆"));
  }

  @Test
  @DisplayName("核心记忆全量取出——不截断")
  void coreMemoryNotTruncated() {
    for (int i = 0; i < 200; i++) {
      store.append("核心条目" + i, MemoryScope.CORE);
    }
    String loaded = store.load();
    assertTrue(loaded.contains("核心条目0"));
    assertTrue(loaded.contains("核心条目199"));
  }

  @Test
  @DisplayName("归档区 LIMIT 100 生效——超过 100 条只取最近 100 条")
  void archiveLimitTakesEffect() {
    for (int i = 0; i < 150; i++) {
      store.append("归档条目" + i, MemoryScope.ARCHIVAL);
    }
    String loaded = store.load();
    assertFalse(loaded.contains("归档条目0"));
    assertTrue(loaded.contains("归档条目149"));
  }

  @Test
  @DisplayName("LIKE 检索——只搜归档区")
  void likeSearchOnlyArchive() {
    store.append("关键词ABC核心", MemoryScope.CORE);
    store.append("关键词ABC归档", MemoryScope.ARCHIVAL);
    List<String> hits = store.recallByKeyword("关键词ABC");
    assertEquals(1, hits.size());
    assertTrue(hits.get(0).contains("关键词ABC归档"));
  }

  @Test
  @DisplayName("recall 未命中返回空列表")
  void recallNoMatchReturnsEmpty() {
    store.append("某条记忆", MemoryScope.ARCHIVAL);
    List<String> hits = store.recallByKeyword("不存在的词");
    assertTrue(hits.isEmpty());
  }

  @Test
  @DisplayName("scope 路由——CORE 和 ARCHIVAL 分别入库")
  void scopeRouting() {
    store.append("核心内容", MemoryScope.CORE);
    store.append("归档内容", MemoryScope.ARCHIVAL);
    List<MemoryEntry> coreEntries = repository.findByScope(MemoryScope.CORE.name());
    List<MemoryEntry> archivalEntries = repository.findByScope(MemoryScope.ARCHIVAL.name());
    assertEquals(1, coreEntries.size());
    assertEquals("核心内容", coreEntries.get(0).getContent());
    assertEquals(1, archivalEntries.size());
    assertEquals("归档内容", archivalEntries.get(0).getContent());
  }
}
