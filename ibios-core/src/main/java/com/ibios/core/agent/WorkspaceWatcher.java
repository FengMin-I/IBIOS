package com.ibios.core.agent;

import com.ibios.core.profile.Profile;
import com.ibios.core.profile.ProfileRegistry;
import com.ibios.core.react.AgentScheduler;
import java.io.IOException;
import java.nio.file.ClosedWatchServiceException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class WorkspaceWatcher {

  private static final Logger LOG = Logger.getLogger(WorkspaceWatcher.class.getName());

  private final Path agentsDir;
  private final AgentLoader agentLoader;
  private final ProfileRegistry profileRegistry;
  private final AgentScheduler agentScheduler;
  private final Predicate<String> providerExists;
  private volatile boolean running;

  public WorkspaceWatcher(
      Path agentsDir,
      AgentLoader agentLoader,
      ProfileRegistry profileRegistry,
      AgentScheduler agentScheduler,
      Predicate<String> providerExists) {
    this.agentsDir = agentsDir;
    this.agentLoader = agentLoader;
    this.profileRegistry = profileRegistry;
    this.agentScheduler = agentScheduler;
    this.providerExists = providerExists;
  }

  public void start() {
    fullScan();
    running = true;

    Thread watchThread = new Thread(this::watchLoop, "workspace-watcher");
    watchThread.setDaemon(true);
    watchThread.start();
  }

  public void stop() {
    running = false;
  }

  void fullScan() {
    if (!Files.isDirectory(agentsDir)) {
      LOG.info("Agents directory does not exist: " + agentsDir);
      return;
    }
    try (Stream<Path> dirs = Files.list(agentsDir)) {
      dirs.filter(Files::isDirectory).forEach(dir -> register(dir));
    } catch (IOException e) {
      LOG.log(Level.SEVERE, "Failed to list agents directory: " + agentsDir, e);
    }
  }

  private void watchLoop() {
    try (WatchService watchService = FileSystems.getDefault().newWatchService()) {
      agentsDir.register(
          watchService,
          StandardWatchEventKinds.ENTRY_CREATE,
          StandardWatchEventKinds.ENTRY_MODIFY,
          StandardWatchEventKinds.ENTRY_DELETE);

      while (running) {
        WatchKey key;
        try {
          key = watchService.take();
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          break;
        } catch (ClosedWatchServiceException e) {
          break;
        }

        for (WatchEvent<?> event : key.pollEvents()) {
          WatchEvent.Kind<?> kind = event.kind();
          if (kind == StandardWatchEventKinds.OVERFLOW) {
            continue;
          }

          @SuppressWarnings("unchecked")
          WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
          Path childPath = agentsDir.resolve(pathEvent.context());

          // 删除事件发生时目录已不存在，isDirectory 会为 false；删除路径必须照样进 onAgentDirChanged
          if (kind == StandardWatchEventKinds.ENTRY_DELETE || Files.isDirectory(childPath)) {
            onAgentDirChanged(childPath, kind);
          }
        }

        boolean valid = key.reset();
        if (!valid) {
          break;
        }
      }
    } catch (IOException e) {
      LOG.log(Level.SEVERE, "Watch service failed", e);
    }
  }

  void onAgentDirChanged(Path agentDir, WatchEvent.Kind<?> kind) {
    try {
      if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
        unregisterByDir(agentDir);
      } else {
        register(agentDir);
      }
    } catch (Exception e) {
      LOG.log(
          Level.WARNING,
          "Agent dir change handling failed for " + agentDir + ": " + e.getMessage(),
          e);
    }
  }

  void register(Path agentDir) {
    try {
      Profile profile = agentLoader.deriveProfile(agentDir);
      if (!providerExists.test(profile.provider().name())) {
        LOG.warning("Unknown provider for agent at " + agentDir + ": " + profile.provider().name());
        return;
      }
      profileRegistry.register(profile);
      if (!profile.schedules().isEmpty()) {
        agentScheduler.registerProfile(profile);
      }
      LOG.info("Registered agent from directory: " + profile.name());
    } catch (Exception e) {
      LOG.log(Level.SEVERE, "Failed to load agent from: " + agentDir, e);
    }
  }

  void unregisterByDir(Path agentDir) {
    String dirName = agentDir.getFileName().toString();
    Profile existing = profileRegistry.get(dirName);
    if (existing != null) {
      agentScheduler.unregisterProfile(existing);
      profileRegistry.remove(dirName);
      LOG.info("Unregistered agent: " + dirName);
    }
  }

  public boolean isRunning() {
    return running;
  }
}
