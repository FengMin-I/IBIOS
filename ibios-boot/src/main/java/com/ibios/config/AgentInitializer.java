package com.ibios.config;

import com.ibios.core.agent.WorkspaceWatcher;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AgentInitializer {
  private static final Logger log = LoggerFactory.getLogger(AgentInitializer.class);

  private final WorkspaceWatcher workspaceWatcher;

  public AgentInitializer(WorkspaceWatcher workspaceWatcher) {
    this.workspaceWatcher = workspaceWatcher;
  }

  @PostConstruct
  public void startWatcher() {
    log.info("Starting workspace watcher for .ibios/agents/");
    workspaceWatcher.start();
  }

  @PreDestroy
  public void stopWatcher() {
    workspaceWatcher.stop();
  }
}
