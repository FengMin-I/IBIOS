package com.ibios.core.agent;

import com.ibios.core.profile.Profile;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public record AgentView(
    String name,
    String description,
    String provider,
    String model,
    List<ScheduleSummary> schedules,
    String agentDir,
    boolean hasScripts,
    boolean hasSkills,
    boolean hasReference,
    boolean hasMemory) {

  public record ScheduleSummary(String id, String cron, String zone, String message) {}

  public static AgentView from(Profile profile) {
    List<ScheduleSummary> scheduleSummaries = new ArrayList<>();
    for (Profile.ScheduleConfig sc : profile.schedules()) {
      scheduleSummaries.add(new ScheduleSummary(sc.id(), sc.cron(), sc.zone(), sc.message()));
    }

    boolean hasScripts =
        profile.agentDir() != null && Files.isDirectory(profile.agentDir().resolve("scripts"));
    boolean hasSkills =
        profile.agentDir() != null && Files.isDirectory(profile.agentDir().resolve("skills"));
    boolean hasReference =
        profile.agentDir() != null
            && Files.isRegularFile(profile.agentDir().resolve("REFERENCE.md"));
    boolean hasMemory =
        profile.agentDir() != null && Files.isRegularFile(profile.agentDir().resolve("MEMORY.md"));

    return new AgentView(
        profile.name(),
        profile.description(),
        profile.provider().name(),
        profile.provider().model(),
        Collections.unmodifiableList(scheduleSummaries),
        profile.agentDir() != null ? profile.agentDir().toString() : null,
        hasScripts,
        hasSkills,
        hasReference,
        hasMemory);
  }
}
