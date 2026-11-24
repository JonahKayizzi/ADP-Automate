package ug.caa.adpautomate.processing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "processing_form_revisions")
public class ProcessingFormRevision {

  @Id
  private UUID id;

  @Column(name = "form_id", nullable = false)
  private UUID formId;

  @Column(name = "revision_number", nullable = false)
  private int revisionNumber;

  @Column(name = "form_code", nullable = false, length = 40)
  private String formCode;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private Map<String, Object> payload = new LinkedHashMap<>();

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(nullable = false, columnDefinition = "jsonb")
  private List<Map<String, Object>> signatures = new ArrayList<>();

  @Column(name = "revised_at", nullable = false)
  private Instant revisedAt;

  @Column(name = "revised_by_account", nullable = false, length = 32)
  private String revisedByAccount;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getFormId() {
    return formId;
  }

  public void setFormId(UUID formId) {
    this.formId = formId;
  }

  public int getRevisionNumber() {
    return revisionNumber;
  }

  public void setRevisionNumber(int revisionNumber) {
    this.revisionNumber = revisionNumber;
  }

  public String getFormCode() {
    return formCode;
  }

  public void setFormCode(String formCode) {
    this.formCode = formCode;
  }

  public Map<String, Object> getPayload() {
    return payload;
  }

  public void setPayload(Map<String, Object> payload) {
    this.payload = payload;
  }

  public List<Map<String, Object>> getSignatures() {
    return signatures;
  }

  public void setSignatures(List<Map<String, Object>> signatures) {
    this.signatures = signatures;
  }

  public Instant getRevisedAt() {
    return revisedAt;
  }

  public void setRevisedAt(Instant revisedAt) {
    this.revisedAt = revisedAt;
  }

  public String getRevisedByAccount() {
    return revisedByAccount;
  }

  public void setRevisedByAccount(String revisedByAccount) {
    this.revisedByAccount = revisedByAccount;
  }
}
