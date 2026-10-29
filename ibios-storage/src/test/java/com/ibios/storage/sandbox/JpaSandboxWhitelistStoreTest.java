package com.ibios.storage.sandbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ibios.core.sandbox.WhitelistEntryView;
import com.ibios.core.sandbox.WhitelistType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaSandboxWhitelistStore.class)
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:sqlite:file:sandbox_whitelist_test?mode=memory&cache=shared",
      "spring.datasource.driver-class-name=org.sqlite.JDBC",
      "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
      "spring.jpa.hibernate.ddl-auto=none",
      "spring.sql.init.mode=always"
    })
class JpaSandboxWhitelistStoreTest {

  @Autowired private JpaSandboxWhitelistStore store;

  @Test
  void shouldCreateAndFindById() {
    WhitelistEntryView created = store.create(WhitelistType.FILE, "/workspace");

    assertNotNull(created.id());
    assertEquals(WhitelistType.FILE, created.type());
    assertEquals("/workspace", created.value());

    WhitelistEntryView found = store.findById(created.id());
    assertEquals(created, found);
    assertTrue(store.exists(WhitelistType.FILE, "/workspace"));
  }

  @Test
  void shouldRejectDuplicateTypeAndValue() {
    store.create(WhitelistType.SHELL, "ls");

    assertThrows(IllegalArgumentException.class, () -> store.create(WhitelistType.SHELL, "ls"));
    store.create(WhitelistType.HTTP, "ls");
  }

  @Test
  void shouldDeleteEntry() {
    WhitelistEntryView created = store.create(WhitelistType.HTTP, "*.example.com");

    assertTrue(store.delete(created.id()));
    assertNull(store.findById(created.id()));
    assertFalse(store.delete(created.id()));
  }

  @Test
  void shouldListAllOrderedByType() {
    store.create(WhitelistType.HTTP, "api.weather.com");
    store.create(WhitelistType.FILE, "/workspace");
    store.create(WhitelistType.SHELL, "ls");

    List<WhitelistEntryView> all = store.listAll();

    assertEquals(3, all.size());
    assertEquals(WhitelistType.FILE, all.get(0).type());
    assertEquals(WhitelistType.HTTP, all.get(1).type());
    assertEquals(WhitelistType.SHELL, all.get(2).type());
  }
}
