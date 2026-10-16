package com.ibios.config;

import com.ibios.core.profile.ProfileLoader;
import jakarta.annotation.PostConstruct;
import java.nio.file.Path;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class ProfileInitializer {
  private static final Logger log = LoggerFactory.getLogger(ProfileInitializer.class);

  private final ProfileLoader profileLoader;

  public ProfileInitializer(ProfileLoader profileLoader) {
    this.profileLoader = profileLoader;
  }

  @PostConstruct
  public void loadProfiles() {
    Path profilesDir = Path.of(".ibios/profiles");
    log.info("Loading profiles from: {}", profilesDir.toAbsolutePath());
    profileLoader.loadAll(profilesDir);
  }
}
