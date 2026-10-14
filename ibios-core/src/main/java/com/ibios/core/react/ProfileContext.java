package com.ibios.core.react;

import com.ibios.core.profile.Profile;

public class ProfileContext {
  private static final ThreadLocal<Profile> CURRENT = new ThreadLocal<>();

  public static void set(Profile profile) {
    CURRENT.set(profile);
  }

  public static Profile current() {
    return CURRENT.get();
  }

  public static void clear() {
    CURRENT.remove();
  }
}
