package com.ibios.core.profile;

public record Profile(
    String name,
    String description,
    Identity identity,
    ProviderConfig provider,
    java.util.List<String> tools,
    java.util.List<String> skills,
    java.util.List<String> mcpServers,
    java.util.List<String> channels,
    java.util.List<NotifyChannelConfig> notifyChannels,
    java.util.List<ScheduleConfig> schedules,
    java.util.List<String> bootstrap,
    java.util.Map<String, Object> settings,
    int maxIterations,
    int maxHistoryTurns) {

  public record Identity(String agentName, String prompt) {}

  public record ProviderConfig(String name, String model, Double temperature) {}

  public record ScheduleConfig(String cron, String zone, String message) {}

  public Profile {
    if (maxIterations <= 0) {
      maxIterations = 10;
    }
    if (maxHistoryTurns <= 0) {
      maxHistoryTurns = 20;
    }
  }
}
