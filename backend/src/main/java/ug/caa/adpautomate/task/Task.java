package ug.caa.adpautomate.task;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tasks")
public class Task {

  @Id
  private UUID id;

  @Column(nullable = false, length = 500)
  private String title;

  @Column(name = "due_date", nullable = false)
  private LocalDate dueDate;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private TaskStatus status;

  @Column(nullable = false)
  private boolean priority = false;

  @Column(name = "assignee_username", length = 32)
  private String assigneeUsername;

  @Column(name = "assignee_name", length = 200)
  private String assigneeName;

  @Column(name = "assignee_designation", length = 200)
  private String assigneeDesignation;

  @Column(name = "assignee_email", length = 320)
  private String assigneeEmail;

  @Column(name = "assigned_at")
  private Instant assignedAt;

  @Column(name = "assigned_by", length = 32)
  private String assignedBy;

  @Column(name = "assigned_by_name", length = 200)
  private String assignedByName;

  @Column(name = "seen_at")
  private Instant seenAt;

  @Column(name = "completed_at")
  private Instant completedAt;

  @Enumerated(EnumType.STRING)
  @Column(name = "notify_status", nullable = false, length = 20)
  private NotifyStatus notifyStatus = NotifyStatus.PENDING;

  @Column(name = "notify_to", length = 32)
  private String notifyTo;

  @Column(name = "notify_at")
  private Instant notifyAt;

  @Column(name = "notify_by", length = 32)
  private String notifyBy;

  @Column(nullable = false)
  private boolean example = false;

  @Column(nullable = false)
  private boolean retired = false;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "created_by")
  private UUID createdBy;

  @Column(name = "created_by_account", length = 32)
  private String createdByAccount;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "updated_by_account", length = 32)
  private String updatedByAccount;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public LocalDate getDueDate() {
    return dueDate;
  }

  public void setDueDate(LocalDate dueDate) {
    this.dueDate = dueDate;
  }

  public TaskStatus getStatus() {
    return status;
  }

  public void setStatus(TaskStatus status) {
    this.status = status;
  }

  public boolean isPriority() {
    return priority;
  }

  public void setPriority(boolean priority) {
    this.priority = priority;
  }

  public String getAssigneeUsername() {
    return assigneeUsername;
  }

  public void setAssigneeUsername(String assigneeUsername) {
    this.assigneeUsername = assigneeUsername;
  }

  public String getAssigneeName() {
    return assigneeName;
  }

  public void setAssigneeName(String assigneeName) {
    this.assigneeName = assigneeName;
  }

  public String getAssigneeDesignation() {
    return assigneeDesignation;
  }

  public void setAssigneeDesignation(String assigneeDesignation) {
    this.assigneeDesignation = assigneeDesignation;
  }

  public String getAssigneeEmail() {
    return assigneeEmail;
  }

  public void setAssigneeEmail(String assigneeEmail) {
    this.assigneeEmail = assigneeEmail;
  }

  public Instant getAssignedAt() {
    return assignedAt;
  }

  public void setAssignedAt(Instant assignedAt) {
    this.assignedAt = assignedAt;
  }

  public String getAssignedBy() {
    return assignedBy;
  }

  public void setAssignedBy(String assignedBy) {
    this.assignedBy = assignedBy;
  }

  public String getAssignedByName() {
    return assignedByName;
  }

  public void setAssignedByName(String assignedByName) {
    this.assignedByName = assignedByName;
  }

  public Instant getSeenAt() {
    return seenAt;
  }

  public void setSeenAt(Instant seenAt) {
    this.seenAt = seenAt;
  }

  public Instant getCompletedAt() {
    return completedAt;
  }

  public void setCompletedAt(Instant completedAt) {
    this.completedAt = completedAt;
  }

  public NotifyStatus getNotifyStatus() {
    return notifyStatus;
  }

  public void setNotifyStatus(NotifyStatus notifyStatus) {
    this.notifyStatus = notifyStatus;
  }

  public String getNotifyTo() {
    return notifyTo;
  }

  public void setNotifyTo(String notifyTo) {
    this.notifyTo = notifyTo;
  }

  public Instant getNotifyAt() {
    return notifyAt;
  }

  public void setNotifyAt(Instant notifyAt) {
    this.notifyAt = notifyAt;
  }

  public String getNotifyBy() {
    return notifyBy;
  }

  public void setNotifyBy(String notifyBy) {
    this.notifyBy = notifyBy;
  }

  public boolean isExample() {
    return example;
  }

  public void setExample(boolean example) {
    this.example = example;
  }

  public boolean isRetired() {
    return retired;
  }

  public void setRetired(boolean retired) {
    this.retired = retired;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public UUID getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(UUID createdBy) {
    this.createdBy = createdBy;
  }

  public String getCreatedByAccount() {
    return createdByAccount;
  }

  public void setCreatedByAccount(String createdByAccount) {
    this.createdByAccount = createdByAccount;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }

  public String getUpdatedByAccount() {
    return updatedByAccount;
  }

  public void setUpdatedByAccount(String updatedByAccount) {
    this.updatedByAccount = updatedByAccount;
  }
}
