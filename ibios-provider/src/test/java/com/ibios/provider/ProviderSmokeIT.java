package com.ibios.provider;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.ibios.core.port.ChatPort.ChatResponse;
import com.ibios.core.port.ChatPort.Prompt;
import com.ibios.core.profile.Profile;
import com.ibios.core.session.Message;
import com.ibios.storage.audit.LlmCall;
import com.ibios.storage.audit.LlmCallRepository;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = ProviderSmokeIT.SmokeTestApplication.class)
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(
    properties = {
      "spring.datasource.url=jdbc:sqlite:file:smoke_test?mode=memory&cache=shared",
      "spring.datasource.driver-class-name=org.sqlite.JDBC",
      "spring.jpa.database-platform=org.hibernate.community.dialect.SQLiteDialect",
      "spring.jpa.hibernate.ddl-auto=none",
      "spring.sql.init.mode=always",
      "ibios.providers[0].name=dashscope",
      "ibios.providers[0].api-key=${DASHSCOPE_API_KEY:}",
      "ibios.providers[0].model=qwen-plus"
    })
@Tag("integration")
class ProviderSmokeIT {

  @SpringBootApplication(scanBasePackages = {"com.ibios.provider", "com.ibios.storage"})
  @EnableJpaRepositories(basePackages = "com.ibios.storage")
  static class SmokeTestApplication {}

  @Autowired private ProviderService providerService;

  @Autowired private LlmCallRepository llmCallRepository;

  @BeforeEach
  void assumeApiKeyPresent() {
    String apiKey = System.getenv("DASHSCOPE_API_KEY");
    Assumptions.assumeTrue(
        apiKey != null && !apiKey.isBlank(),
        "DASHSCOPE_API_KEY not set; skipping integration test");
  }

  @Test
  void shouldMakeRealCallAndRecordAudit() {
    Profile profile =
        new Profile(
            "smoke-test",
            "Smoke test profile",
            new Profile.Identity("TestAgent", "You are a helpful assistant."),
            new Profile.ProviderConfig("dashscope", "qwen-plus", null),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            List.of(),
            Map.of(),
            10,
            20,
            null);

    Prompt prompt =
        new Prompt(
            "system", List.of(new Message(Message.Role.USER, "Say hello in one word.")), null);
    ChatResponse response = providerService.chat("smoke-session", profile, prompt);

    assertNotNull(response);
    assertNotNull(response.text());
    assertFalse(response.text().isBlank(), "Response text should not be blank");

    List<LlmCall> calls = llmCallRepository.findAll();
    assertFalse(calls.isEmpty(), "llm_calls should have at least one record");
    LlmCall lastCall = calls.get(calls.size() - 1);
    assertNotNull(lastCall.getSuccess());
    assertNotNull(lastCall.getCalledAt());
  }
}
