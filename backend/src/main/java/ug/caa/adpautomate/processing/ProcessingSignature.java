package ug.caa.adpautomate.processing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processing_signatures")
public class ProcessingSignature {

  @Id
  private UUID id;

  @Column(name = "form_id", nullable = false)
  private UUID formId;

  @Column(name = "slot_key", nullable = false, length = 64)
  private String slotKey;

  @Column(name = "slot_label", nullable = false, length = 120)
  private String slotLabel;

  @Column(name = "signer_name", length = 200)
  private String signerName;

  @Column(length = 200)
  private String designation;

  @Column(name = "signed_at")
  private Instant signedAt;

  @Column(name = "signed_by")
  private UUID signedBy;

  @Column(name = "signed_by_account", length = 32)
  private String signedByAccount;

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

  public String getSlotKey() {
    return slotKey;
  }

  public void setSlotKey(String slotKey) {
    this.slotKey = slotKey;
  }

  public String getSlotLabel() {
    return slotLabel;
  }

  public void setSlotLabel(String slotLabel) {
    this.slotLabel = slotLabel;
  }

  public String getSignerName() {
    return signerName;
  }

  public void setSignerName(String signerName) {
    this.signerName = signerName;
  }

  public String getDesignation() {
    return designation;
  }

  public void setDesignation(String designation) {
    this.designation = designation;
  }

  public Instant getSignedAt() {
    return signedAt;
  }

  public void setSignedAt(Instant signedAt) {
    this.signedAt = signedAt;
  }

  public UUID getSignedBy() {
    return signedBy;
  }

  public void setSignedBy(UUID signedBy) {
    this.signedBy = signedBy;
  }

  public String getSignedByAccount() {
    return signedByAccount;
  }

  public void setSignedByAccount(String signedByAccount) {
    this.signedByAccount = signedByAccount;
  }

  public boolean isSigned() {
    return signedAt != null;
  }
}
