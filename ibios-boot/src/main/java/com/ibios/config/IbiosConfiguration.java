package com.ibios.config;

import com.ibios.core.agent.AgentLifecycleService;
import com.ibios.core.agent.AgentLoader;
import com.ibios.core.agent.AgentStore;
import com.ibios.core.agent.WorkspaceWatcher;
import com.ibios.core.audit.ToolInvocationAudit;
import com.ibios.core.audit.ToolUsageQuery;
import com.ibios.core.memory.MemoryService;
import com.ibios.core.port.ChatPort;
import com.ibios.core.port.ToolRegistry;
import com.ibios.core.profile.ProfileLoader;
import com.ibios.core.profile.ProfileRegistry;
import com.ibios.core.provider.ProviderStore;
import com.ibios.core.react.AgentScheduler;
import com.ibios.core.react.AgentService;
import com.ibios.core.react.ContextLoader;
import com.ibios.core.react.PromptBuilder;
import com.ibios.core.react.ReActLoop;
import com.ibios.core.react.ToolExecutor;
import com.ibios.core.session.SessionManager;
import com.ibios.core.workspace.WorkspaceRoot;
import com.ibios.provider.ProviderChatModelFactory;
import com.ibios.provider.ProviderService;
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
import java.util.function.Predicate;
import org.springframework.beans.factory.annotation.Value;
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

  /** 工作区根：serve/gateway 走 Spring 属性 {@code ibios.root}，与轻命令共用同一套目录约定。 */
  @Bean
  public WorkspaceRoot workspaceRoot(@Value("${ibios.root:.ibios}") String root) {
    return new WorkspaceRoot(root);
  }

  @Bean
  public ProfileRegistry profileRegistry() {
    return new ProfileRegistry();
  }

  @Bean
  public ProfileLoader profileLoader(ProfileRegistry registry, ProviderStore providerStore) {
    return new ProfileLoader(registry, providerExists(providerStore));
  }

  /** Provider 存在性每次现查 DB：管理页增删改后无需重启，Agent 校验立即生效。 */
  private static Predicate<String> providerExists(ProviderStore providerStore) {
    return name -> name != null && providerStore.getByName(name) != null;
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
  public PromptBuilder promptBuilder(
      ContextLoader contextLoader, MemoryService memoryService, ToolRegistry toolRegistry) {
    return new PromptBuilder(contextLoader, memoryService, toolRegistry);
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
  public AgentStore agentStore(WorkspaceRoot workspaceRoot) {
    Path agentsDir = workspaceRoot.agentsDir().toAbsolutePath().normalize();
    Path archiveDir = workspaceRoot.archiveDir().toAbsolutePath().normalize();
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
      ToolUsageQuery toolUsageQuery,
      ProviderStore providerStore,
      IbiosAuthorProperties authorProperties) {
    String agentAuthorPrompt = loadAgentAuthorPrompt();
    return new AgentLifecycleService(
        agentStore,
        agentLoader,
        profileRegistry,
        agentScheduler,
        chatPort,
        sessionManager,
        toolUsageQuery,
        providerExists(providerStore),
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
      ProviderStore providerStore) {
    return new WorkspaceWatcher(
        agentStore.agentsDir(),
        agentLoader,
        profileRegistry,
        agentScheduler,
        providerExists(providerStore));
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
