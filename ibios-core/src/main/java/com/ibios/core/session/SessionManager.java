package com.ibios.core.session;

import java.util.List;

public interface SessionManager {

  Session getOrCreate(String channel, String userId, String profileName);

  Session get(String sessionId);

  void save(Session session);

  List<Session> listAll();
}
