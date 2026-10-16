package com.ibios.core.schedule;

import java.time.Instant;

public record ScheduledTaskView(
    String taskId,
    String profileName,
    String cron,
    String zone,
    String message,
    boolean enabled,
    Instant nextRunAt,
    Instant lastRunAt,
    String lastStatus,
    int runCount,
    Instant createdAt) {}
