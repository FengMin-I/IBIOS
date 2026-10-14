package com.ibios.core.profile;

import java.util.List;
import java.util.Map;

public record Profile(
    String name,
    String description,
    Identity identity,
    ProviderConfig provider,
    List<String> tools,
    List<String> skills,
    List<String> mcpServers,
    List<String> channels,
    List<String> notifyChannels,
    List<ScheduleConfig> schedules,
    List<String> bootstrap,
    Map<String, Object> settings) {

  public record Identity(String agentName, String prompt) {}

  public record ProviderConfig(String name, String model, Double temperature) {}

  public record ScheduleConfig(String cron, String zone, String message) {}
}
