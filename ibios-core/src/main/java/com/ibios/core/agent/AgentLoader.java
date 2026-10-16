package com.ibios.core.agent;

import com.ibios.core.profile.NotifyChannelConfig;
import com.ibios.core.profile.Profile;
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

public class AgentLoader {

  private static final Logger LOG = Logger.getLogger(AgentLoader.class.getName());
  private static final Pattern FRONTMATTER_PATTERN =
      Pattern.compile("\\A---\\s*\\n(.*?)\\n---\\s*\\n(.*)\\z", Pattern.DOTALL);
  private static final Pattern ENV_PATTERN = Pattern.compile("\\$\\{([^}]+)}");

  public AgentDefinition load(Path agentDir) {
    Path agentMd = agentDir.resolve("AGENT.md");
    if (!Files.isRegularFile(agentMd)) {
      throw new IllegalArgumentException("AGENT.md not found in: " + agentDir);
    }

    String raw;
    try {
      raw = Files.readString(agentMd);
    } catch (IOException e) {
      throw new IllegalArgumentException("Failed to read AGENT.md: " + agentMd, e);
    }

    Matcher matcher = FRONTMATTER_PATTERN.matcher(raw);
    if (!matcher.matches()) {
      throw new IllegalArgumentException(
          "AGENT.md missing frontmatter (must start with --- and end with ---): " + agentMd);
    }

    String yamlContent = matcher.group(1);
    String body = matcher.group(2).trim();
    String resolved = resolveEnvVars(yamlContent);

    Yaml yaml = new Yaml();
    Map<String, Object> frontmatter = yaml.load(resolved);
    if (frontmatter == null) {
      frontmatter = Collections.emptyMap();
    }

    String name = (String) frontmatter.get("name");
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("AGENT.md frontmatter missing required field: name");
    }

    Object providerObj = frontmatter.get("provider");
    if (providerObj == null) {
      throw new IllegalArgumentException("AGENT.md frontmatter missing required field: provider");
    }

    Path scriptsDir = agentDir.resolve("scripts");
    Path skillsDir = agentDir.resolve("skills");
    Path referenceFile = agentDir.resolve("REFERENCE.md");

    return new AgentDefinition(
        name,
        frontmatter,
        body,
        agentDir.toAbsolutePath(),
        Files.isDirectory(scriptsDir) ? scriptsDir : null,
        Files.isDirectory(skillsDir) ? skillsDir : null,
        Files.isRegularFile(referenceFile) ? referenceFile : null);
  }

  public Profile deriveProfile(Path agentDir) {
    AgentDefinition def = load(agentDir);
    Map<String, Object> fm = def.frontmatter();

    String description = (String) fm.getOrDefault("description", "");
    Profile.Identity identity = convertIdentity(asMap(fm.get("identity")));
    Profile.ProviderConfig provider = convertProviderConfig(asMap(fm.get("provider")));
    List<String> tools = asStringList(fm.get("tools"));
    List<String> skills = asStringList(fm.get("skills"));
    List<String> mcpServers = asStringList(fm.get("mcp_servers"));
    List<String> channels = asStringList(fm.get("channels"));
    List<NotifyChannelConfig> notifyChannels =
        convertNotifyChannels(asList(fm.get("notify_channels")));
    List<Profile.ScheduleConfig> schedules = convertSchedules(asList(fm.get("schedules")));
    List<String> bootstrap = asStringList(fm.get("bootstrap"));
    Map<String, Object> settings = asObjectMap(fm.get("settings"));
    int maxIterations = asInt(fm.get("max_iterations"), 10);
    int maxHistoryTurns = asInt(fm.get("max_history_turns"), 20);

    return new Profile(
        def.name(),
        description,
        identity,
        provider,
        tools,
        skills,
        mcpServers,
        channels,
        notifyChannels,
        schedules,
        bootstrap,
        settings,
        maxIterations,
        maxHistoryTurns,
        def.agentDir());
  }

  public void scanAndRegister(
      Path agentsDir,
      com.ibios.core.profile.ProfileRegistry registry,
      com.ibios.core.react.AgentScheduler scheduler,
      Set<String> validProviderNames,
      Set<String> registeredToolNames) {
    if (!Files.isDirectory(agentsDir)) {
      LOG.info("Agents directory does not exist: " + agentsDir);
      return;
    }
    try (Stream<Path> dirs = Files.list(agentsDir)) {
      dirs.filter(Files::isDirectory)
          .forEach(
              dir -> {
                try {
                  Profile profile = deriveProfile(dir);
                  if (!validProviderNames.contains(profile.provider().name())) {
                    throw new IllegalArgumentException(
                        "Unknown provider: " + profile.provider().name());
                  }
                  for (String toolName : profile.tools()) {
                    if (!registeredToolNames.contains(toolName)) {
                      LOG.warning(
                          "Agent '"
                              + profile.name()
                              + "' references unregistered tool: "
                              + toolName);
                    }
                  }
                  registry.register(profile);
                  if (scheduler != null) {
                    scheduler.registerProfile(profile);
                  }
                  LOG.info("Registered agent from directory: " + profile.name());
                } catch (Exception e) {
                  LOG.log(Level.SEVERE, "Failed to load agent from: " + dir, e);
                }
              });
    } catch (IOException e) {
      LOG.log(Level.SEVERE, "Failed to list agents directory: " + agentsDir, e);
    }
  }

  static String resolveEnvVars(String content) {
    Matcher matcher = ENV_PATTERN.matcher(content);
    StringBuilder sb = new StringBuilder();
    while (matcher.find()) {
      String varName = matcher.group(1);
      String value = System.getenv(varName);
      if (value == null) {
        throw new IllegalStateException("Environment variable not set: " + varName);
      }
      matcher.appendReplacement(sb, Matcher.quoteReplacement(value));
    }
    matcher.appendTail(sb);
    return sb.toString();
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> asMap(Object obj) {
    if (obj instanceof Map) {
      return (Map<String, Object>) obj;
    }
    return null;
  }

  @SuppressWarnings("unchecked")
  private List<Map<String, Object>> asList(Object obj) {
    if (obj instanceof List) {
      return (List<Map<String, Object>>) obj;
    }
    return null;
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

  private List<NotifyChannelConfig> convertNotifyChannels(List<Map<String, Object>> list) {
    if (list == null) {
      return Collections.emptyList();
    }
    List<NotifyChannelConfig> result = new ArrayList<>();
    for (Map<String, Object> item : list) {
      result.add(
          new NotifyChannelConfig(
              (String) item.getOrDefault("name", ""),
              (String) item.getOrDefault("type", ""),
              (String) item.getOrDefault("url", "")));
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
