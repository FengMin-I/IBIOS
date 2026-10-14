package com.ibios.core.session;

public interface SessionManager {

  Session getOrCreate(String channel, String userId, String profileName);

  Session get(String sessionId);

  void save(Session session);
}
