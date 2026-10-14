package com.ibios.storage.audit;

import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class JpaLlmCallAudit implements LlmCallAudit {

  private final LlmCallRepository repository;

  public JpaLlmCallAudit(LlmCallRepository repository) {
    this.repository = repository;
  }

  @Override
  public void record(
      String sessionId,
      String provider,
      String model,
      TokenUsage usage,
      boolean success,
      String errorMessage,
      long latencyMs) {
    LlmCall call = new LlmCall();
    call.setSessionId(sessionId);
    call.setProvider(provider);
    call.setModel(model);
    if (usage != null) {
      call.setPromptTokens(usage.promptTokens());
      call.setCompletionTokens(usage.completionTokens());
      call.setTotalTokens(usage.totalTokens());
    }
    call.setLatencyMs(latencyMs);
    call.setSuccess(success);
    call.setErrorMessage(errorMessage);
    call.setCalledAt(Instant.now().toString());
    repository.save(call);
  }
}
