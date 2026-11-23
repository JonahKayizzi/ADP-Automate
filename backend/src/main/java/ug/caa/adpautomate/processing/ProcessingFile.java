package ug.caa.adpautomate.processing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processing_files")
public class ProcessingFile {

  @Id
  private UUID id;

  @Enumerated(EnumType.STRING)
  @Column(name = "owner_type", nullable = false, length = 32)
  private ProcessingOwnerType ownerType;

  @Column(name = "owner_id", nullable = false)
  private UUID ownerId;

  @Column(name = "record_kind", nullable = false, length = 40)
  private String recordKind;

  @Column(nullable = false)
  private boolean retired = false;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public ProcessingOwnerType getOwnerType() {
    return ownerType;
  }

  public void setOwnerType(ProcessingOwnerType ownerType) {
    this.ownerType = ownerType;
  }

  public UUID getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(UUID ownerId) {
    this.ownerId = ownerId;
  }

  public String getRecordKind() {
    return recordKind;
  }

  public void setRecordKind(String recordKind) {
    this.recordKind = recordKind;
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

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}
