package com.ibios.config;

import com.ibios.core.profile.ProfileLoader;
import com.ibios.core.workspace.WorkspaceRoot;
import jakarta.annotation.PostConstruct;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ProfileInitializer {
  private static final Logger log = LoggerFactory.getLogger(ProfileInitializer.class);

  private final ProfileLoader profileLoader;
  private final WorkspaceRoot workspaceRoot;

  public ProfileInitializer(ProfileLoader profileLoader, WorkspaceRoot workspaceRoot) {
    this.profileLoader = profileLoader;
    this.workspaceRoot = workspaceRoot;
  }

  @PostConstruct
  public void loadProfiles() {
    Path profilesDir = workspaceRoot.profilesDir();
    log.info("Loading profiles from: {}", profilesDir.toAbsolutePath());
    profileLoader.loadAll(profilesDir);
  }
}
