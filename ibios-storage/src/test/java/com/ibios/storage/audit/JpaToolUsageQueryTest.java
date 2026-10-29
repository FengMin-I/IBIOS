package com.ibios.storage.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ibios.storage.session.SessionEntity;
import com.ibios.storage.session.SessionRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

/**
 * 该查询跨 audit 与 session 两个包的实体，而本包的 TestApplication 只扫描 audit，
 * 因此显式声明一份放宽扫描的测试配置（不用 @SpringBootConfiguration，避免同包出现两个候选配置）。
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = JpaToolUsageQueryTest.CrossPackageConfig.class)
@Import(JpaToolUsageQuery.class)
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:sqlite:file:tool_usage_test?mode=memory&cache=shared",
      "spring.datasource.driver-class-name=org.sqlite.JDBC",
      "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
      "spring.jpa.hibernate.ddl-auto=none",
      "spring.sql.init.mode=always"
    })
class JpaToolUsageQueryTest {

  @Configuration
  @EnableAutoConfiguration
  @EntityScan(basePackages = "com.ibios.storage")
  @EnableJpaRepositories(basePackages = "com.ibios.storage")
  static class CrossPackageConfig {}

  @Autowired private JpaToolUsageQuery query;
  @Autowired private SessionRepository sessionRepository;
  @Autowired private ToolInvocationRepository toolInvocationRepository;

  @Test
  void shouldGroupDistinctToolNamesByProfile() {
    givenSession("s-weather", "weather-daily");
    givenSession("s-weather-2", "weather-daily");
    givenSession("s-reconcile", "daily-reconcile");

    givenInvocation("s-weather", "notify");
    givenInvocation("s-weather", "http_get");
    givenInvocation("s-weather-2", "notify");
    givenInvocation("s-weather-2", "save_memory");
    givenInvocation("s-reconcile", "shell");

    Map<String, List<String>> used = query.findUsedToolNamesByProfile();

    assertEquals(List.of("http_get", "notify", "save_memory"), used.get("weather-daily"));
    assertEquals(List.of("shell"), used.get("daily-reconcile"));
    assertEquals(2, used.size());
  }

  @Test
  void shouldReturnEmptyMapWhenNothingRecorded() {
    assertTrue(query.findUsedToolNamesByProfile().isEmpty());
  }

  @Test
  void shouldIgnoreInvocationsWithoutMatchingSession() {
    givenSession("s-known", "weather-daily");
    givenInvocation("s-known", "notify");
    givenInvocation("s-orphan", "shell");

    Map<String, List<String>> used = query.findUsedToolNamesByProfile();

    assertEquals(List.of("notify"), used.get("weather-daily"));
    assertEquals(1, used.size());
    assertTrue(used.values().stream().noneMatch(tools -> tools.contains("shell")));
  }

  @Test
  void shouldCountFailedInvocationsAsUsed() {
    givenSession("s-fail", "daily-reconcile");
    givenInvocation("s-fail", "http_post", false, "timeout");

    assertEquals(List.of("http_post"), query.findUsedToolNamesByProfile().get("daily-reconcile"));
  }

  private void givenSession(String sessionId, String profileName) {
    SessionEntity session = new SessionEntity();
    session.setSessionId(sessionId);
    session.setProfileName(profileName);
    session.setChannel("cli");
    session.setUserId("test-user");
    session.setMessagesJson("[]");
    session.setStatus("active");
    session.setCreatedAt("2026-10-29T08:00:00Z");
    session.setLastActiveAt("2026-10-29T08:00:00Z");
    sessionRepository.save(session);
  }

  private void givenInvocation(String sessionId, String toolName) {
    givenInvocation(sessionId, toolName, true, null);
  }

  private void givenInvocation(
      String sessionId, String toolName, boolean success, String errorMessage) {
    ToolInvocation invocation = new ToolInvocation();
    invocation.setSessionId(sessionId);
    invocation.setToolName(toolName);
    invocation.setInputJson("{}");
    invocation.setOutputJson("{}");
    invocation.setSuccess(success);
    invocation.setErrorMessage(errorMessage);
    invocation.setDurationMs(10L);
    invocation.setCreatedAt("2026-10-29T08:00:00Z");
    toolInvocationRepository.save(invocation);
  }
}
