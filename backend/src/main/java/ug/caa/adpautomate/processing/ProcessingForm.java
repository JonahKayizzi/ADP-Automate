package ug.caa.adpautomate.processing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "processing_forms")
public class ProcessingForm {

  @Id
  private UUID id;

  @Column(name = "processing_file_id", nullable = false)
  private UUID processingFileId;

  @Column(name = "step_number", nullable = false)
  private int stepNumber;

  @Column(name = "form_code", nullable = false, length = 40)
  private String formCode;

  @Column(name = "form_title", nullable = false, length = 200)
  private String formTitle;

  @Column(name = "controlled_notice", nullable = false, length = 500)
  private String controlledNotice;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> payload = new LinkedHashMap<>();

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private ProcessingFormStatus status = ProcessingFormStatus.NOT_STARTED;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

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

  public UUID getProcessingFileId() {
    return processingFileId;
  }

  public void setProcessingFileId(UUID processingFileId) {
    this.processingFileId = processingFileId;
  }

  public int getStepNumber() {
    return stepNumber;
  }

  public void setStepNumber(int stepNumber) {
    this.stepNumber = stepNumber;
  }

  public String getFormCode() {
    return formCode;
  }

  public void setFormCode(String formCode) {
    this.formCode = formCode;
  }

  public String getFormTitle() {
    return formTitle;
  }

  public void setFormTitle(String formTitle) {
    this.formTitle = formTitle;
  }

  public String getControlledNotice() {
    return controlledNotice;
  }

  public void setControlledNotice(String controlledNotice) {
    this.controlledNotice = controlledNotice;
  }

  public Map<String, Object> getPayload() {
    return payload;
  }

  public void setPayload(Map<String, Object> payload) {
    this.payload = payload;
  }

  public ProcessingFormStatus getStatus() {
    return status;
  }

  public void setStatus(ProcessingFormStatus status) {
    this.status = status;
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

  public String getUpdatedByAccount() {
    return updatedByAccount;
  }

  public void setUpdatedByAccount(String updatedByAccount) {
    this.updatedByAccount = updatedByAccount;
  }
}
