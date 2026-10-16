package com.ibios.storage.schedule;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "scheduled_tasks")
public class ScheduledTask {

  @Id
  @Column(name = "task_id")
  private String taskId;

  @Column(name = "profile_name", nullable = false)
  private String profileName;

  @Column(nullable = false)
  private String cron;

  @Column(nullable = false)
  private String zone;

  @Column(nullable = false)
  private String message;

  @Column(nullable = false)
  private Boolean enabled = true;

  @Column(name = "next_run_at")
  private Instant nextRunAt;

  @Column(name = "last_run_at")
  private Instant lastRunAt;

  @Column(name = "last_status")
  private String lastStatus;

  @Column(name = "run_count", nullable = false)
  private Integer runCount = 0;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  public ScheduledTask() {}

  public String getTaskId() {
    return taskId;
  }

  public void setTaskId(String taskId) {
    this.taskId = taskId;
  }

  public String getProfileName() {
    return profileName;
  }

  public void setProfileName(String profileName) {
    this.profileName = profileName;
  }

  public String getCron() {
    return cron;
  }

  public void setCron(String cron) {
    this.cron = cron;
  }

  public String getZone() {
    return zone;
  }

  public void setZone(String zone) {
    this.zone = zone;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public Instant getNextRunAt() {
    return nextRunAt;
  }

  public void setNextRunAt(Instant nextRunAt) {
    this.nextRunAt = nextRunAt;
  }

  public Instant getLastRunAt() {
    return lastRunAt;
  }

  public void setLastRunAt(Instant lastRunAt) {
    this.lastRunAt = lastRunAt;
  }

  public String getLastStatus() {
    return lastStatus;
  }

  public void setLastStatus(String lastStatus) {
    this.lastStatus = lastStatus;
  }

  public Integer getRunCount() {
    return runCount;
  }

  public void setRunCount(Integer runCount) {
    this.runCount = runCount;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
