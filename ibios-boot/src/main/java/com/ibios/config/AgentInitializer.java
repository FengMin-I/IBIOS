package com.ibios.config;

import com.ibios.core.agent.AgentLoader;
import com.ibios.core.port.ToolRegistry;
import com.ibios.core.profile.ProfileRegistry;
import com.ibios.core.react.AgentScheduler;
import com.ibios.provider.ProvidersProperties;
import jakarta.annotation.PostConstruct;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AgentInitializer {
  private static final Logger log = LoggerFactory.getLogger(AgentInitializer.class);

  private final AgentLoader agentLoader;
  private final ProfileRegistry profileRegistry;
  private final AgentScheduler agentScheduler;
  private final ToolRegistry toolRegistry;
  private final Set<String> validProviderNames;

  public AgentInitializer(
      AgentLoader agentLoader,
      ProfileRegistry profileRegistry,
      AgentScheduler agentScheduler,
      ToolRegistry toolRegistry,
      ProvidersProperties properties) {
    this.agentLoader = agentLoader;
    this.profileRegistry = profileRegistry;
    this.agentScheduler = agentScheduler;
    this.toolRegistry = toolRegistry;
    this.validProviderNames =
        properties.getProviders().stream()
            .map(ProvidersProperties.ProviderEntry::getName)
            .collect(Collectors.toSet());
  }

  @PostConstruct
  public void scanAgents() {
    Path agentsDir = Path.of(".ibios/agents");
    log.info("Scanning agents from: {}", agentsDir.toAbsolutePath());
    Set<String> registeredToolNames =
        toolRegistry.listAll().stream().map(t -> t.name()).collect(Collectors.toSet());
    agentLoader.scanAndRegister(
        agentsDir, profileRegistry, agentScheduler, validProviderNames, registeredToolNames);
  }
}
