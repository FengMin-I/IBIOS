package com.ibios.core.profile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.yaml.snakeyaml.Yaml;

public class ProfileLoader {

  private static final Logger LOG = Logger.getLogger(ProfileLoader.class.getName());
  private static final Pattern ENV_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

  private final ProfileRegistry registry;
  private final Set<String> validProviderNames;

  public ProfileLoader(ProfileRegistry registry, Set<String> validProviderNames) {
    this.registry = registry;
    this.validProviderNames = validProviderNames;
  }

  public void loadAll(Path profilesDir) {
    if (!Files.isDirectory(profilesDir)) {
      LOG.info("Profiles directory does not exist: " + profilesDir);
      return;
    }
    try (Stream<Path> files = Files.list(profilesDir)) {
      files
          .filter(
              p -> {
                String name = p.getFileName().toString();
                return name.endsWith(".yaml") || name.endsWith(".yml");
              })
          .forEach(this::loadOne);
    } catch (IOException e) {
      LOG.log(Level.SEVERE, "Failed to list profiles directory: " + profilesDir, e);
    }
  }

  private void loadOne(Path file) {
    try {
      String raw = Files.readString(file);
      String resolved = resolveEnvVars(raw);
      Yaml yaml = new Yaml();
      Map<String, Object> data = yaml.load(resolved);
      if (data == null) {
        LOG.warning("Empty profile file: " + file);
        return;
      }
      Profile profile = convertToProfile(data);
      if (!validProviderNames.contains(profile.provider().name())) {
        throw new ProviderNotFoundException(profile.provider().name());
      }
      registry.register(profile);
      LOG.info("Loaded profile: " + profile.name());
    } catch (Exception e) {
      LOG.log(Level.SEVERE, "Failed to load profile: " + file, e);
    }
  }

  static String resolveEnvVars(String content) {
    Matcher matcher = ENV_PATTERN.matcher(content);
    StringBuilder sb = new StringBuilder();
    while (matcher.find()) {
      String varName = matcher.group(1);
      String value = System.getenv(varName);
      if (value == null) {
        value = "";
      }
      matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
    }
    matcher.appendTail(sb);
    return sb.toString();
  }

  @SuppressWarnings("unchecked")
  private Profile convertToProfile(Map<String, Object> data) {
    String name = (String) data.get("name");
    String description = (String) data.getOrDefault("description", "");
    Profile.Identity identity = convertIdentity((Map<String, Object>) data.get("identity"));
    Profile.ProviderConfig provider =
        convertProviderConfig((Map<String, Object>) data.get("provider"));
    List<String> skills = asStringList(data.get("skills"));
    List<String> mcpServers = asStringList(data.get("mcp_servers"));
    List<String> channels = asStringList(data.get("channels"));
    List<Profile.ScheduleConfig> schedules =
        convertSchedules((List<Map<String, Object>>) data.get("schedules"));
    List<String> bootstrap = asStringList(data.get("bootstrap"));
    Map<String, Object> settings = asObjectMap(data.get("settings"));
    int maxIterations = asInt(data.get("max_iterations"), 10);
    int maxHistoryTurns = asInt(data.get("max_history_turns"), 20);
    return new Profile(
        name,
        description,
        identity,
        provider,
        skills,
        mcpServers,
        channels,
        schedules,
        bootstrap,
        settings,
        maxIterations,
        maxHistoryTurns,
        null);
  }

  private Profile.Identity convertIdentity(Map<String, Object> map) {
    if (map == null) {
      return new Profile.Identity("", "");
    }
    return new Profile.Identity(
        (String) map.getOrDefault("agent_name", ""), (String) map.getOrDefault("prompt", ""));
  }

  private Profile.ProviderConfig convertProviderConfig(Map<String, Object> map) {
    if (map == null) {
      return new Profile.ProviderConfig("", "", null);
    }
    Object tempObj = map.get("temperature");
    Double temperature = null;
    if (tempObj instanceof Number) {
      temperature = ((Number) tempObj).doubleValue();
    }
    return new Profile.ProviderConfig(
        (String) map.get("name"), (String) map.getOrDefault("model", ""), temperature);
  }

  private List<Profile.ScheduleConfig> convertSchedules(List<Map<String, Object>> list) {
    if (list == null) {
      return Collections.emptyList();
    }
    List<Profile.ScheduleConfig> result = new ArrayList<>();
    for (Map<String, Object> item : list) {
      result.add(
          new Profile.ScheduleConfig(
              (String) item.getOrDefault("id", ""),
              (String) item.getOrDefault("cron", ""),
              (String) item.getOrDefault("zone", ""),
              (String) item.getOrDefault("message", "")));
    }
    return result;
  }

  private List<String> asStringList(Object obj) {
    if (obj == null) {
      return Collections.emptyList();
    }
    if (obj instanceof List) {
      List<String> result = new ArrayList<>();
      for (Object item : (List<?>) obj) {
        result.add(String.valueOf(item));
      }
      return result;
    }
    return Collections.emptyList();
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> asObjectMap(Object obj) {
    if (obj == null) {
      return Collections.emptyMap();
    }
    if (obj instanceof Map) {
      return new HashMap<>((Map<String, Object>) obj);
    }
    return Collections.emptyMap();
  }

  private int asInt(Object obj, int defaultValue) {
    if (obj == null) {
      return defaultValue;
    }
    if (obj instanceof Number) {
      return ((Number) obj).intValue();
    }
    return defaultValue;
  }
}
