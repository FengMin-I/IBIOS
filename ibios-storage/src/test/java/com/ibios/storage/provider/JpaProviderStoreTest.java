package com.ibios.storage.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ibios.core.provider.ProviderView;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaProviderStore.class)
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:sqlite:file:provider_store_test?mode=memory&cache=shared",
      "spring.datasource.driver-class-name=org.sqlite.JDBC",
      "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
      "spring.jpa.hibernate.ddl-auto=none",
      "spring.sql.init.mode=always"
    })
class JpaProviderStoreTest {

  @Autowired private JpaProviderStore store;

  @Test
  void shouldCreateAndFindByName() {
    ProviderView created =
        store.create(
            "deepseek", "openai", "sk-test-1", "https://api.deepseek.com", "deepseek-chat");

    assertNotNull(created.id());
    ProviderView found = store.getByName("deepseek");
    assertNotNull(found);
    assertEquals("openai", found.type());
    assertEquals("sk-test-1", found.apiKey());
    assertEquals("https://api.deepseek.com", found.baseUrl());
    assertEquals("deepseek-chat", found.model());
  }

  @Test
  void shouldRejectDuplicateName() {
    store.create("deepseek", "openai", "sk-test-1", null, null);

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> store.create("deepseek", "openai", "sk-test-2", null, null));
    assertTrue(ex.getMessage().contains("already exists"));
  }

  @Test
  void shouldUpdateFieldsButKeepName() {
    ProviderView created = store.create("deepseek", "openai", "sk-test-1", null, "deepseek-chat");
    ProviderView updated =
        store.update(
            created.id(), "dashscope", "sk-test-2", "https://dashscope.aliyuncs.com", "qwen-plus");

    assertNotNull(updated);
    assertEquals("deepseek", updated.name());
    assertEquals("dashscope", updated.type());
    assertEquals("sk-test-2", updated.apiKey());
    assertEquals("https://dashscope.aliyuncs.com", updated.baseUrl());
    assertEquals("qwen-plus", updated.model());
  }

  @Test
  void shouldDeleteAndReportEmptiness() {
    assertTrue(store.isEmpty());
    ProviderView created = store.create("deepseek", "openai", "sk-test-1", null, null);
    assertFalse(store.isEmpty());

    store.delete(created.id());
    assertNull(store.getById(created.id()));
    assertTrue(store.listAll().isEmpty());
  }

  @Test
  void shouldListAllProviders() {
    store.create("deepseek", "openai", "sk-test-1", null, null);
    store.create("dashscope", "dashscope", "sk-test-2", null, null);

    List<ProviderView> all = store.listAll();
    assertEquals(2, all.size());
  }
}
