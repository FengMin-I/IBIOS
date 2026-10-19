package com.ibios.web.model;

import java.util.List;

public record AgentView(
    String name,
    String description,
    String provider,
    String model,
    List<String> tools,
    List<ScheduleSummary> schedules,
    String agentDir,
    boolean hasScripts,
    boolean hasSkills,
    boolean hasReference,
    boolean hasMemory) {

  public record ScheduleSummary(String id, String cron, String zone, String message) {}
}
