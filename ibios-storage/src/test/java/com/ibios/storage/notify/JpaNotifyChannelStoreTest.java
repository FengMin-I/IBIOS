package com.ibios.storage.notify;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ibios.core.notify.NotifyChannelView;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaNotifyChannelStore.class)
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:sqlite:file:notify_store_test?mode=memory&cache=shared",
      "spring.datasource.driver-class-name=org.sqlite.JDBC",
      "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
      "spring.jpa.hibernate.ddl-auto=none",
      "spring.sql.init.mode=always"
    })
class JpaNotifyChannelStoreTest {

  @Autowired private JpaNotifyChannelStore store;

  @Test
  void shouldCreateAndFindByName() {
    NotifyChannelView created =
        store.create("team-webhook", "webhook", "https://example.com/hook", "团队群");

    assertNotNull(created.id());
    NotifyChannelView found = store.getByName("team-webhook");
    assertNotNull(found);
    assertEquals("webhook", found.type());
    assertEquals("https://example.com/hook", found.url());
    assertEquals("团队群", found.description());
  }

  @Test
  void shouldRejectDuplicateName() {
    store.create("team-webhook", "webhook", "https://example.com/hook", "");

    IllegalArgumentException ex =
        assertThrows(
            IllegalArgumentException.class,
            () -> store.create("team-webhook", "webhook", "https://other.com/hook", ""));
    assertTrue(ex.getMessage().contains("already exists"));
  }

  @Test
  void shouldUpdateAndDelete() {
    NotifyChannelView created =
        store.create("team-webhook", "webhook", "https://example.com/hook", "");
    NotifyChannelView updated =
        store.update(created.id(), "team-lark", "feishu", "https://example.com/new", "新群");

    assertNotNull(updated);
    assertEquals("team-lark", updated.name());
    assertEquals("feishu", updated.type());
    assertNull(store.getByName("team-webhook"));

    store.delete(created.id());
    assertNull(store.getById(created.id()));
    assertTrue(store.listAll().isEmpty());
  }

  @Test
  void shouldListAllChannels() {
    store.create("a", "webhook", "https://a.example.com", "");
    store.create("b", "feishu", "https://b.example.com", "");

    List<NotifyChannelView> all = store.listAll();
    assertEquals(2, all.size());
  }
}
