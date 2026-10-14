package com.ibios.storage.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:sqlite:file:session_repo_test?mode=memory&cache=shared",
      "spring.datasource.driver-class-name=org.sqlite.JDBC",
      "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
      "spring.jpa.hibernate.ddl-auto=none",
      "spring.sql.init.mode=always"
    })
class SessionRepositoryTest {

  @Autowired private SessionRepository repository;

  @Test
  void shouldSaveAndFindById() {
    SessionEntity entity = new SessionEntity();
    entity.setSessionId("cli:wang:default");
    entity.setProfileName("default");
    entity.setChannel("cli");
    entity.setUserId("wang");
    entity.setMessagesJson("[]");
    entity.setStatus("active");
    entity.setCreatedAt("2026-10-14T10:00:00Z");
    entity.setLastActiveAt("2026-10-14T10:00:00Z");

    repository.save(entity);

    Optional<SessionEntity> found = repository.findById("cli:wang:default");
    assertTrue(found.isPresent());
    assertEquals("default", found.get().getProfileName());
    assertEquals("cli", found.get().getChannel());
    assertEquals("wang", found.get().getUserId());
    assertEquals("[]", found.get().getMessagesJson());
  }

  @Test
  void shouldPersistMessagesJson() {
    String messagesJson =
        "[{\"role\":\"USER\",\"content\":\"Hello\"},{\"role\":\"ASSISTANT\",\"content\":\"Hi\"}]";
    SessionEntity entity = new SessionEntity();
    entity.setSessionId("cli:wang:default");
    entity.setProfileName("default");
    entity.setChannel("cli");
    entity.setUserId("wang");
    entity.setMessagesJson(messagesJson);
    entity.setStatus("active");
    entity.setCreatedAt("2026-10-14T10:00:00Z");
    entity.setLastActiveAt("2026-10-14T10:00:00Z");

    repository.save(entity);

    SessionEntity loaded = repository.findById("cli:wang:default").orElseThrow();
    assertNotNull(loaded.getMessagesJson());
    assertTrue(loaded.getMessagesJson().contains("Hello"));
    assertTrue(loaded.getMessagesJson().contains("Hi"));
  }

  @Test
  void shouldUpdateMessagesJson() {
    SessionEntity entity = new SessionEntity();
    entity.setSessionId("cli:wang:default");
    entity.setProfileName("default");
    entity.setChannel("cli");
    entity.setUserId("wang");
    entity.setMessagesJson("[]");
    entity.setStatus("active");
    entity.setCreatedAt("2026-10-14T10:00:00Z");
    entity.setLastActiveAt("2026-10-14T10:00:00Z");
    repository.save(entity);

    entity.setMessagesJson("[{\"role\":\"USER\",\"content\":\"Hello\"}]");
    repository.save(entity);

    SessionEntity loaded = repository.findById("cli:wang:default").orElseThrow();
    assertTrue(loaded.getMessagesJson().contains("Hello"));
  }
}
