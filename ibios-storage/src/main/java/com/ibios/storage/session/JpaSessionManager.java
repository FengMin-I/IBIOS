package com.ibios.storage.session;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ibios.core.session.Message;
import com.ibios.core.session.Session;
import com.ibios.core.session.SessionManager;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class JpaSessionManager implements SessionManager {

  private final SessionRepository repository;
  private final ObjectMapper objectMapper;

  public JpaSessionManager(SessionRepository repository) {
    this.repository = repository;
    this.objectMapper = new ObjectMapper();
  }

  @Override
  public Session getOrCreate(String channel, String userId, String profileName) {
    String sessionId = buildSessionId(channel, userId, profileName);
    return repository
        .findById(sessionId)
        .map(this::toDomain)
        .orElseGet(() -> createNew(sessionId, profileName, channel, userId));
  }

  @Override
  public Session get(String sessionId) {
    return repository.findById(sessionId).map(this::toDomain).orElse(null);
  }

  @Override
  public void save(Session session) {
    SessionEntity entity = repository.findById(session.id()).orElse(new SessionEntity());
    entity.setSessionId(session.id());
    entity.setProfileName(session.profileName());
    entity.setChannel(session.channel());
    entity.setUserId(session.userId());
    entity.setMessagesJson(serializeMessages(session.messages()));
    entity.setLastActiveAt(Instant.now().toString());
    if (entity.getCreatedAt() == null) {
      entity.setCreatedAt(Instant.now().toString());
    }
    repository.save(entity);
  }

  String buildSessionId(String channel, String userId, String profileName) {
    return channel + ":" + userId + ":" + profileName;
  }

  private Session createNew(String sessionId, String profileName, String channel, String userId) {
    Session session = new Session(sessionId, profileName, channel, userId);
    SessionEntity entity = new SessionEntity();
    entity.setSessionId(sessionId);
    entity.setProfileName(profileName);
    entity.setChannel(channel);
    entity.setUserId(userId);
    entity.setMessagesJson("[]");
    entity.setStatus("active");
    String now = Instant.now().toString();
    entity.setCreatedAt(now);
    entity.setLastActiveAt(now);
    repository.save(entity);
    return session;
  }

  private Session toDomain(SessionEntity entity) {
    Session session =
        new Session(
            entity.getSessionId(),
            entity.getProfileName(),
            entity.getChannel(),
            entity.getUserId());
    List<Message> messages = deserializeMessages(entity.getMessagesJson());
    for (Message msg : messages) {
      switch (msg.role()) {
        case USER:
          session.appendUserMessage(msg.content());
          break;
        case ASSISTANT:
          session.appendAssistantMessage(msg.content(), Collections.emptyList());
          break;
        case TOOL:
          session.appendToolResult(
              new com.ibios.core.session.ToolResult("", "", msg.content(), true, null));
          break;
        default:
          break;
      }
    }
    return session;
  }

  private String serializeMessages(List<Message> messages) {
    try {
      return objectMapper.writeValueAsString(messages);
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to serialize messages", e);
    }
  }

  private List<Message> deserializeMessages(String json) {
    if (json == null || json.isBlank()) {
      return Collections.emptyList();
    }
    try {
      return objectMapper.readValue(json, new TypeReference<List<Message>>() {});
    } catch (JsonProcessingException e) {
      throw new IllegalStateException("Failed to deserialize messages", e);
    }
  }
}
