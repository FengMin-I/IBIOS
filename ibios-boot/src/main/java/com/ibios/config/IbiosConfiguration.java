package com.ibios.config;

import com.ibios.core.agent.AgentLifecycleService;
import com.ibios.core.agent.AgentLoader;
import com.ibios.core.agent.AgentStore;
import com.ibios.core.agent.WorkspaceWatcher;
import com.ibios.core.audit.ToolInvocationAudit;
import com.ibios.core.memory.MemoryService;
import com.ibios.core.port.ChatPort;
import com.ibios.core.port.ToolRegistry;
import com.ibios.core.profile.ProfileLoader;
import com.ibios.core.profile.ProfileRegistry;
import com.ibios.core.react.AgentScheduler;
import com.ibios.core.react.AgentService;
import com.ibios.core.react.ContextLoader;
import com.ibios.core.react.PromptBuilder;
import com.ibios.core.react.ReActLoop;
import com.ibios.core.react.ToolExecutor;
import com.ibios.core.session.SessionManager;
import com.ibios.provider.ProviderChatModelFactory;
import com.ibios.provider.ProviderService;
import com.ibios.provider.ProvidersProperties;
import com.ibios.provider.ToolSchemaAdapter;
import com.ibios.storage.audit.LlmCallAudit;
import com.ibios.storage.session.JpaSessionManager;
import com.ibios.storage.session.SessionRepository;
import com.ibios.tool.registry.DefaultToolRegistry;
import com.ibios.tool.sandbox.FileSandboxProperties;
import com.ibios.tool.sandbox.HttpSandboxProperties;
import com.ibios.tool.sandbox.ShellSandboxProperties;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
  FileSandboxProperties.class,
  ShellSandboxProperties.class,
  HttpSandboxProperties.class,
  IbiosAuthorProperties.class
})
public class IbiosConfiguration {

  @Bean
  public ProfileRegistry profileRegistry() {
    return new ProfileRegistry();
  }

  @Bean
  public ProfileLoader profileLoader(ProfileRegistry registry, ProvidersProperties properties) {
    Set<String> validProviderNames =
        properties.getProviders().stream()
            .map(ProvidersProperties.ProviderEntry::getName)
            .collect(Collectors.toSet());
    return new ProfileLoader(registry, validProviderNames);
  }

  @Bean
  public AgentLoader agentLoader() {
    return new AgentLoader();
  }

  @Bean
  public ContextLoader contextLoader() {
    return new ContextLoader();
  }

  @Bean
  public PromptBuilder promptBuilder(ContextLoader contextLoader, MemoryService memoryService) {
    return new PromptBuilder(contextLoader, memoryService);
  }

  @Bean
  public ToolRegistry toolRegistry() {
    return new DefaultToolRegistry();
  }

  @Bean
  public ToolExecutor toolExecutor(ToolRegistry toolRegistry, ToolInvocationAudit audit) {
    return new ToolExecutor(toolRegistry, audit);
  }

  @Bean
  public ProviderChatModelFactory providerChatModelFactory(ProvidersProperties properties) {
    return new ProviderChatModelFactory(properties);
  }

  @Bean
  public ToolSchemaAdapter toolSchemaAdapter() {
    return new ToolSchemaAdapter();
  }

  @Bean
  public ChatPort chatPort(
      ProviderChatModelFactory factory, ToolSchemaAdapter toolSchemaAdapter, LlmCallAudit audit) {
    return new ProviderService(factory, toolSchemaAdapter, audit);
  }

  @Bean
  public ReActLoop reActLoop(
      PromptBuilder promptBuilder, ChatPort chatPort, ToolExecutor toolExecutor) {
    return new ReActLoop(promptBuilder, chatPort, toolExecutor);
  }

  @Bean
  public SessionManager sessionManager(SessionRepository repository) {
    return new JpaSessionManager(repository);
  }

  @Bean
  public AgentService agentService(
      ProfileRegistry profileRegistry, ReActLoop reActLoop, SessionManager sessionManager) {
    return new AgentService(profileRegistry, reActLoop, sessionManager);
  }

  @Bean
  public AgentStore agentStore() {
    Path agentsDir = Path.of(".ibios/agents").toAbsolutePath();
    Path archiveDir = Path.of(".ibios/archive").toAbsolutePath();
    try {
      Files.createDirectories(agentsDir);
      Files.createDirectories(archiveDir);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to create agent directories", e);
    }
    return new AgentStore(agentsDir, archiveDir);
  }

  @Bean
  public AgentLifecycleService agentLifecycleService(
      AgentStore agentStore,
      AgentLoader agentLoader,
      ProfileRegistry profileRegistry,
      AgentScheduler agentScheduler,
      ChatPort chatPort,
      SessionManager sessionManager,
      ProvidersProperties providersProperties,
      IbiosAuthorProperties authorProperties) {
    Set<String> validProviderNames =
        providersProperties.getProviders().stream()
            .map(ProvidersProperties.ProviderEntry::getName)
            .collect(Collectors.toSet());
    String agentAuthorPrompt = loadAgentAuthorPrompt();
    return new AgentLifecycleService(
        agentStore,
        agentLoader,
        profileRegistry,
        agentScheduler,
        chatPort,
        sessionManager,
        validProviderNames,
        authorProperties.getProvider(),
        authorProperties.getModel(),
        agentAuthorPrompt);
  }

  @Bean
  public WorkspaceWatcher workspaceWatcher(
      AgentStore agentStore,
      AgentLoader agentLoader,
      ProfileRegistry profileRegistry,
      AgentScheduler agentScheduler,
      ProvidersProperties providersProperties,
      ToolRegistry toolRegistry) {
    Set<String> validProviderNames =
        providersProperties.getProviders().stream()
            .map(ProvidersProperties.ProviderEntry::getName)
            .collect(Collectors.toSet());
    Set<String> registeredToolNames =
        toolRegistry.listAll().stream()
            .map(com.ibios.core.tool.IbiosTool::name)
            .collect(Collectors.toSet());
    return new WorkspaceWatcher(
        agentStore.agentsDir(),
        agentLoader,
        profileRegistry,
        agentScheduler,
        validProviderNames,
        registeredToolNames);
  }

  private String loadAgentAuthorPrompt() {
    Path promptFile = Path.of("docs/prompt/prompt.md");
    if (!Files.exists(promptFile)) {
      return "Generate a valid AGENT.md file based on the user description.";
    }
    try {
      return Files.readString(promptFile);
    } catch (IOException e) {
      return "Generate a valid AGENT.md file based on the user description.";
    }
  }
}
