package com.ibios.storage.session;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "sessions")
public class SessionEntity {

  @Id
  @Column(name = "session_id")
  private String sessionId;

  @Column(name = "profile_name", nullable = false)
  private String profileName;

  @Column(name = "channel", nullable = false)
  private String channel;

  @Column(name = "user_id", nullable = false)
  private String userId;

  @Column(name = "messages_json")
  private String messagesJson;

  @Column(name = "status", nullable = false)
  private String status = "active";

  @Column(name = "created_at", nullable = false)
  private String createdAt;

  @Column(name = "last_active_at", nullable = false)
  private String lastActiveAt;

  @Column(name = "archived_at")
  private String archivedAt;

  public SessionEntity() {}

  public String getSessionId() {
    return sessionId;
  }

  public void setSessionId(String sessionId) {
    this.sessionId = sessionId;
  }

  public String getProfileName() {
    return profileName;
  }

  public void setProfileName(String profileName) {
    this.profileName = profileName;
  }

  public String getChannel() {
    return channel;
  }

  public void setChannel(String channel) {
    this.channel = channel;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getMessagesJson() {
    return messagesJson;
  }

  public void setMessagesJson(String messagesJson) {
    this.messagesJson = messagesJson;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(String createdAt) {
    this.createdAt = createdAt;
  }

  public String getLastActiveAt() {
    return lastActiveAt;
  }

  public void setLastActiveAt(String lastActiveAt) {
    this.lastActiveAt = lastActiveAt;
  }

  public String getArchivedAt() {
    return archivedAt;
  }

  public void setArchivedAt(String archivedAt) {
    this.archivedAt = archivedAt;
  }
}
