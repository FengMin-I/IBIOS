package com.ibios.config;

import com.ibios.core.agent.WorkspaceWatcher;
import com.ibios.core.workspace.WorkspaceRoot;
import com.ibios.provider.ProviderSeeder;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AgentInitializer {
  private static final Logger log = LoggerFactory.getLogger(AgentInitializer.class);

  private final WorkspaceWatcher workspaceWatcher;
  private final ProviderSeeder providerSeeder;
  private final WorkspaceRoot workspaceRoot;

  public AgentInitializer(
      WorkspaceWatcher workspaceWatcher,
      ProviderSeeder providerSeeder,
      WorkspaceRoot workspaceRoot) {
    this.workspaceWatcher = workspaceWatcher;
    this.providerSeeder = providerSeeder;
    this.workspaceRoot = workspaceRoot;
  }

  @PostConstruct
  public void startWatcher() {
    // 先播种 Provider：Agent 扫描时要用 provider 名做校验，顺序颠倒会让首启的 Agent 全部失败
    providerSeeder.seed();
    log.info("Starting workspace watcher for {}/", workspaceRoot.agentsDir());
    workspaceWatcher.start();
  }

  @PreDestroy
  public void stopWatcher() {
    workspaceWatcher.stop();
  }
}
