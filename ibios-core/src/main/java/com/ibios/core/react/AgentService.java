package com.ibios.core.react;

import com.ibios.core.profile.Profile;
import com.ibios.core.profile.ProfileRegistry;
import com.ibios.core.session.Session;
import com.ibios.core.session.SessionManager;

public class AgentService {
  private final ProfileRegistry profileRegistry;
  private final ReActLoop reActLoop;
  private final SessionManager sessionManager;

  public AgentService(
      ProfileRegistry profileRegistry, ReActLoop reActLoop, SessionManager sessionManager) {
    this.profileRegistry = profileRegistry;
    this.reActLoop = reActLoop;
    this.sessionManager = sessionManager;
  }

  public String process(Session session, String userMessage) {
    Profile profile = profileRegistry.get(session.profileName());
    ProfileContext.set(profile);
    try {
      String reply = reActLoop.run(session, userMessage, profile);
      sessionManager.save(session);
      return reply;
    } finally {
      ProfileContext.clear();
    }
  }
}
