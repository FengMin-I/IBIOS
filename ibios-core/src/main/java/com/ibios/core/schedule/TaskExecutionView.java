package com.ibios.core.schedule;

import java.time.Instant;

public record TaskExecutionView(
    String taskId,
    String sessionId,
    Instant startedAt,
    boolean success,
    String errorMessage,
    Long durationMs) {}
