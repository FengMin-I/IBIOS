package com.ibios.storage.session;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.ibios.core.session.Session;
import com.ibios.core.session.SessionManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaSessionManager.class)
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:sqlite:file:session_mgr_test?mode=memory&cache=shared",
      "spring.datasource.driver-class-name=org.sqlite.JDBC",
      "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
      "spring.jpa.hibernate.ddl-auto=none",
      "spring.sql.init.mode=always"
    })
class SessionManagerTest {

  @Autowired private SessionManager sessionManager;

  @Test
  void sameTripleShouldReturnSameSession() {
    Session first = sessionManager.getOrCreate("cli", "wang", "default");
    Session second = sessionManager.getOrCreate("cli", "wang", "default");

    assertEquals(first.id(), second.id());
  }

  @Test
  void differentChannelShouldReturnDifferentSession() {
    Session cli = sessionManager.getOrCreate("cli", "wang", "default");
    Session web = sessionManager.getOrCreate("web", "wang", "default");

    assertNotEquals(cli.id(), web.id());
  }

  @Test
  void differentUserShouldReturnDifferentSession() {
    Session wang = sessionManager.getOrCreate("cli", "wang", "default");
    Session li = sessionManager.getOrCreate("cli", "li", "default");

    assertNotEquals(wang.id(), li.id());
  }

  @Test
  void differentProfileShouldReturnDifferentSession() {
    Session defaultProfile = sessionManager.getOrCreate("cli", "wang", "default");
    Session weatherProfile = sessionManager.getOrCreate("cli", "wang", "weather");

    assertNotEquals(defaultProfile.id(), weatherProfile.id());
  }

  @Test
  void sessionIdShouldOnlyBeGeneratedInsideSessionManager() {
    Session session = sessionManager.getOrCreate("cli", "wang", "default");
    assertNotNull(session.id());
    assertEquals("cli:wang:default", session.id());
  }
}
