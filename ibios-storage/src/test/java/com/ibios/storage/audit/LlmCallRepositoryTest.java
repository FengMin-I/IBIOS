package com.ibios.storage.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
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
      "spring.datasource.url=jdbc:sqlite:mem:ibios_test",
      "spring.datasource.driver-class-name=org.sqlite.JDBC",
      "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
      "spring.jpa.hibernate.ddl-auto=none",
      "spring.sql.init.mode=always"
    })
class LlmCallRepositoryTest {

  @Autowired private LlmCallRepository repository;

  @Test
  void shouldSaveAndFindById() {
    LlmCall call = createCall("session-1", "dashscope", "qwen-plus", 10, 20, 30, 150L, true, null);

    LlmCall saved = repository.save(call);
    assertNotNull(saved.getId());

    Optional<LlmCall> found = repository.findById(saved.getId());
    assertTrue(found.isPresent());
    assertEquals("session-1", found.get().getSessionId());
    assertEquals("dashscope", found.get().getProvider());
    assertEquals("qwen-plus", found.get().getModel());
    assertEquals(10, found.get().getPromptTokens());
    assertEquals(20, found.get().getCompletionTokens());
    assertEquals(30, found.get().getTotalTokens());
    assertEquals(150L, found.get().getLatencyMs());
    assertTrue(found.get().getSuccess());
    assertNull(found.get().getErrorMessage());
  }

  @Test
  void shouldSaveFailedCall() {
    LlmCall call =
        createCall(
            "session-2", "openai", "gpt-4", null, null, null, 50L, false, "Rate limit exceeded");

    LlmCall saved = repository.save(call);
    Optional<LlmCall> found = repository.findById(saved.getId());
    assertTrue(found.isPresent());
    assertEquals(false, found.get().getSuccess());
    assertEquals("Rate limit exceeded", found.get().getErrorMessage());
    assertNull(found.get().getPromptTokens());
  }

  @Test
  void shouldListAllCalls() {
    repository.save(createCall("s1", "dashscope", "qwen-plus", 1, 1, 2, 10L, true, null));
    repository.save(createCall("s2", "dashscope", "qwen-max", 2, 2, 4, 20L, true, null));

    List<LlmCall> all = repository.findAll();
    assertEquals(2, all.size());
  }

  private LlmCall createCall(
      String sessionId,
      String provider,
      String model,
      Integer promptTokens,
      Integer completionTokens,
      Integer totalTokens,
      Long latencyMs,
      Boolean success,
      String errorMessage) {
    LlmCall call = new LlmCall();
    call.setSessionId(sessionId);
    call.setProvider(provider);
    call.setModel(model);
    call.setPromptTokens(promptTokens);
    call.setCompletionTokens(completionTokens);
    call.setTotalTokens(totalTokens);
    call.setLatencyMs(latencyMs);
    call.setSuccess(success);
    call.setErrorMessage(errorMessage);
    call.setCalledAt("2026-10-14T10:00:00Z");
    return call;
  }
}
