package com.ibios.core.profile;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ProfileRegistry {

  private final Map<String, Profile> profiles = new ConcurrentHashMap<>();

  public Profile get(String name) {
    return profiles.get(name);
  }

  public Collection<Profile> getAll() {
    return Collections.unmodifiableCollection(profiles.values());
  }

  public void register(Profile profile) {
    profiles.put(profile.name(), profile);
  }

  public void remove(String name) {
    profiles.remove(name);
  }

  public boolean exists(String name) {
    return profiles.containsKey(name);
  }
}
