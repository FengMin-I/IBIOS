package com.ibios.core.agent;

import com.ibios.core.port.ChatPort;
import com.ibios.core.profile.Profile;
import com.ibios.core.profile.ProfileRegistry;
import com.ibios.core.react.AgentScheduler;
import com.ibios.core.session.Message;
import com.ibios.core.session.SessionManager;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
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

public class AgentLifecycleService {

  private static final Logger LOG = Logger.getLogger(AgentLifecycleService.class.getName());

  private final AgentStore agentStore;
  private final AgentLoader agentLoader;
  private final ProfileRegistry profileRegistry;
  private final AgentScheduler agentScheduler;
  private final ChatPort chatPort;
  private final SessionManager sessionManager;
  private final Set<String> validProviderNames;
  private final String authorProvider;
  private final String authorModel;
  private final String agentAuthorPrompt;

  public AgentLifecycleService(
      AgentStore agentStore,
      AgentLoader agentLoader,
      ProfileRegistry profileRegistry,
      AgentScheduler agentScheduler,
      ChatPort chatPort,
      SessionManager sessionManager,
      Set<String> validProviderNames,
      String authorProvider,
      String authorModel,
      String agentAuthorPrompt) {
    this.agentStore = agentStore;
    this.agentLoader = agentLoader;
    this.profileRegistry = profileRegistry;
    this.agentScheduler = agentScheduler;
    this.chatPort = chatPort;
    this.sessionManager = sessionManager;
    this.validProviderNames = validProviderNames;
    this.authorProvider = authorProvider;
    this.authorModel = authorModel;
    this.agentAuthorPrompt = agentAuthorPrompt;
  }

  public AgentView create(String name, String description) {
    if (profileRegistry.exists(name)) {
      throw new IllegalArgumentException("Agent already exists: " + name);
    }

    Path agentDir = agentStore.agentsDir().resolve(name);
    try {
      String template = loadTemplate("agent-templates/AGENT.md.tpl");
      String agentMd =
          template
              .replace("${name}", name)
              .replace("${description}", description)
              .replace("${provider}", authorProvider)
              .replace("${model}", authorModel);

      String referenceTemplate = loadTemplate("agent-templates/REFERENCE.md.tpl");

      Map<String, String> files = new HashMap<>();
      files.put("AGENT.md", agentMd);
      files.put("REFERENCE.md", referenceTemplate);

      agentStore.writeAll(name, files);

      Profile profile = register(agentDir);
      return AgentView.from(profile);
    } catch (Exception e) {
      LOG.log(Level.SEVERE, "Failed to create agent: " + name, e);
      agentStore.delete(agentDir);
      throw new IllegalStateException("Failed to create agent: " + name, e);
    }
  }

  Profile register(Path agentDir) {
    Profile profile = agentLoader.deriveProfile(agentDir);
    if (!validProviderNames.contains(profile.provider().name())) {
      throw new IllegalArgumentException("Unknown provider: " + profile.provider().name());
    }
    profileRegistry.register(profile);
    if (!profile.schedules().isEmpty()) {
      agentScheduler.registerProfile(profile);
    }
    LOG.info("Registered agent: " + profile.name());
    return profile;
  }

  public AgentView get(String name) {
    Profile profile = profileRegistry.get(name);
    if (profile == null) {
      throw new IllegalArgumentException("Agent not found: " + name);
    }
    return AgentView.from(profile);
  }

  public List<AgentView> list() {
    List<AgentView> views = new ArrayList<>();
    for (Profile profile : profileRegistry.getAll()) {
      views.add(AgentView.from(profile));
    }
    return views;
  }

  public AgentView update(String name, String markdown) {
    Profile existing = profileRegistry.get(name);
    if (existing == null) {
      throw new IllegalArgumentException("Agent not found: " + name);
    }

    Path agentDir = agentStore.agentsDir().resolve(name);
    try {
      Files.writeString(agentDir.resolve("AGENT.md"), markdown);

      if (!existing.schedules().isEmpty()) {
        agentScheduler.unregisterProfile(existing);
      }

      Profile newProfile = register(agentDir);
      return AgentView.from(newProfile);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to update agent: " + name, e);
    }
  }

  public void delete(String name) {
    Profile profile = profileRegistry.get(name);
    if (profile == null) {
      throw new IllegalArgumentException("Agent not found: " + name);
    }

    agentScheduler.unregisterProfile(profile);
    profileRegistry.remove(name);
    agentStore.archive(name);
    LOG.info("Deleted agent: " + name);
  }

  public String readAgentMemory(String name) {
    Profile profile = profileRegistry.get(name);
    if (profile == null) {
      throw new IllegalArgumentException("Agent not found: " + name);
    }
    if (profile.agentDir() == null) {
      return "";
    }
    Path memoryFile = profile.agentDir().resolve("MEMORY.md");
    if (!Files.isRegularFile(memoryFile)) {
      return "";
    }
    try {
      return Files.readString(memoryFile);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to read memory file for agent: " + name, e);
    }
  }

  public void saveAgentMemory(String name, String content) {
    Profile profile = profileRegistry.get(name);
    if (profile == null) {
      throw new IllegalArgumentException("Agent not found: " + name);
    }
    if (profile.agentDir() == null) {
      throw new IllegalArgumentException("Agent directory not available: " + name);
    }
    Path memoryFile = profile.agentDir().resolve("MEMORY.md");
    try {
      Files.writeString(memoryFile, content != null ? content : "");
    } catch (IOException e) {
      throw new IllegalStateException("Failed to save memory file for agent: " + name, e);
    }
  }

  public Map<String, String> regenerateAgent(String name, String description) {
    Profile profile = profileRegistry.get(name);
    if (profile == null) {
      throw new IllegalArgumentException("Agent not found: " + name);
    }

    Profile authorProfile =
        new Profile(
            "author",
            "Agent author",
            new Profile.Identity("Agent Author", agentAuthorPrompt),
            new Profile.ProviderConfig(authorProvider, authorModel, null),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyMap(),
            10,
            20,
            null);

    List<Message> messages = new ArrayList<>();
    messages.add(new Message(Message.Role.USER, description));

    ChatPort.Prompt prompt =
        new ChatPort.Prompt(agentAuthorPrompt, messages, Collections.emptyList());

    ChatPort.ChatResponse response = chatPort.chat("regenerate-" + name, authorProfile, prompt);
    String content = response.text();

    if (content == null || content.isBlank()) {
      throw new IllegalStateException("LLM returned empty response for agent regeneration");
    }

    content = stripCodeFences(content);

    Map<String, String> files = new HashMap<>();
    files.put("AGENT.md", content);
    return files;
  }

  public Map<String, String> generateFiles(String name, String description) {
    if (profileRegistry.exists(name)) {
      throw new IllegalArgumentException("Agent already exists: " + name);
    }

    Profile authorProfile =
        new Profile(
            "author",
            "Agent author",
            new Profile.Identity("Agent Author", agentAuthorPrompt),
            new Profile.ProviderConfig(authorProvider, authorModel, null),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyMap(),
            10,
            20,
            null);

    List<Message> messages = new ArrayList<>();
    messages.add(new Message(Message.Role.USER, description));

    ChatPort.Prompt prompt =
        new ChatPort.Prompt(agentAuthorPrompt, messages, Collections.emptyList());

    ChatPort.ChatResponse response = chatPort.chat("generate-" + name, authorProfile, prompt);
    String content = response.text();

    if (content == null || content.isBlank()) {
      throw new IllegalStateException("LLM returned empty response for agent generation");
    }

    content = stripCodeFences(content);

    Map<String, String> files = new HashMap<>();
    files.put("AGENT.md", content);

    Path tempDir = null;
    try {
      tempDir = Files.createTempDirectory("agent-validate-");
      Files.writeString(tempDir.resolve("AGENT.md"), content);
      agentLoader.deriveProfile(tempDir);
    } catch (Exception e) {
      throw new IllegalStateException("Generated AGENT.md is not valid: " + e.getMessage(), e);
    } finally {
      if (tempDir != null) {
        try {
          Files.walk(tempDir)
              .sorted(java.util.Comparator.reverseOrder())
              .forEach(
                  path -> {
                    try {
                      Files.deleteIfExists(path);
                    } catch (IOException ex) {
                      LOG.log(Level.WARNING, "Failed to delete temp file: " + path, ex);
                    }
                  });
        } catch (IOException e) {
          LOG.log(Level.WARNING, "Failed to clean up temp directory: " + tempDir, e);
        }
      }
    }

    return files;
  }

  public AgentView saveFiles(String name, Map<String, String> files) {
    String agentMd = files.get("AGENT.md");
    if (agentMd == null || agentMd.isBlank()) {
      throw new IllegalArgumentException("files must contain AGENT.md");
    }

    Path tempDir = null;
    try {
      tempDir = Files.createTempDirectory("agent-validate-");
      Files.writeString(tempDir.resolve("AGENT.md"), agentMd);
      agentLoader.deriveProfile(tempDir);
    } catch (Exception e) {
      throw new IllegalStateException("AGENT.md is not valid: " + e.getMessage(), e);
    } finally {
      if (tempDir != null) {
        try {
          Files.walk(tempDir)
              .sorted(java.util.Comparator.reverseOrder())
              .forEach(
                  path -> {
                    try {
                      Files.deleteIfExists(path);
                    } catch (IOException ex) {
                      LOG.log(Level.WARNING, "Failed to delete temp file: " + path, ex);
                    }
                  });
        } catch (IOException e) {
          LOG.log(Level.WARNING, "Failed to clean up temp directory: " + tempDir, e);
        }
      }
    }

    Profile existing = profileRegistry.get(name);
    if (existing != null && !existing.schedules().isEmpty()) {
      agentScheduler.unregisterProfile(existing);
    }

    agentStore.writeAll(name, files);
    Path agentDir = agentStore.agentsDir().resolve(name);
    Profile profile = register(agentDir);
    return AgentView.from(profile);
  }

  private String loadTemplate(String resourcePath) {
    try (InputStream is = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
      if (is == null) {
        throw new IllegalStateException("Template not found: " + resourcePath);
      }
      return new String(is.readAllBytes(), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load template: " + resourcePath, e);
    }
  }

  private String stripCodeFences(String content) {
    if (content.startsWith("```")) {
      int firstNewline = content.indexOf('\n');
      if (firstNewline > 0) {
        content = content.substring(firstNewline + 1);
      }
      if (content.endsWith("```")) {
        content = content.substring(0, content.length() - 3);
      }
      return content.trim();
    }
    return content;
  }
}
