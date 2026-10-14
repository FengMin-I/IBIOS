package com.ibios.storage.audit;

import com.ibios.core.audit.ToolInvocationAudit;
import java.time.Instant;
import org.springframework.stereotype.Component;

@Component
public class JpaToolInvocationAudit implements ToolInvocationAudit {
  private final ToolInvocationRepository repository;

  public JpaToolInvocationAudit(ToolInvocationRepository repository) {
    this.repository = repository;
  }

  @Override
  public void recordToolInvocation(
      String sessionId,
      String toolName,
      String inputJson,
      String outputJson,
      boolean success,
      String errorMessage,
      long durationMs) {
    ToolInvocation invocation = new ToolInvocation();
    invocation.setSessionId(sessionId);
    invocation.setToolName(toolName);
    invocation.setInputJson(inputJson);
    invocation.setOutputJson(outputJson);
    invocation.setSuccess(success);
    invocation.setErrorMessage(errorMessage);
    invocation.setDurationMs(durationMs);
    invocation.setCreatedAt(Instant.now().toString());
    repository.save(invocation);
  }
}
