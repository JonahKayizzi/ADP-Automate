package ug.caa.adpautomate.complaint;

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
@Table(name = "complaints")
public class Complaint {

  @Id
  private UUID id;

  @Column(nullable = false, length = 500)
  private String title;

  @Column(nullable = false, length = 300)
  private String complainant;

  @Column(nullable = false)
  private LocalDate received;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 40)
  private ComplaintChannel channel;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ComplaintStatus status;

  @Column(columnDefinition = "text")
  private String description;

  @Column(name = "action_taken", columnDefinition = "text")
  private String actionTaken;

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

  public String getComplainant() {
    return complainant;
  }

  public void setComplainant(String complainant) {
    this.complainant = complainant;
  }

  public LocalDate getReceived() {
    return received;
  }

  public void setReceived(LocalDate received) {
    this.received = received;
  }

  public ComplaintChannel getChannel() {
    return channel;
  }

  public void setChannel(ComplaintChannel channel) {
    this.channel = channel;
  }

  public ComplaintStatus getStatus() {
    return status;
  }

  public void setStatus(ComplaintStatus status) {
    this.status = status;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getActionTaken() {
    return actionTaken;
  }

  public void setActionTaken(String actionTaken) {
    this.actionTaken = actionTaken;
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
