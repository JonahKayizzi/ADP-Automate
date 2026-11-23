package ug.caa.adpautomate.notam;

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
@Table(name = "notams")
public class Notam {

  @Id
  private UUID id;

  @Column(nullable = false, length = 16)
  private String number;

  @Column(nullable = false)
  private LocalDate issued;

  @Column(nullable = false, length = 500)
  private String title;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 80)
  private NotamOriginator originator;

  @Column(name = "valid_to", length = 32)
  private String validTo;

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

  public String getNumber() {
    return number;
  }

  public void setNumber(String number) {
    this.number = number;
  }

  public LocalDate getIssued() {
    return issued;
  }

  public void setIssued(LocalDate issued) {
    this.issued = issued;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public NotamOriginator getOriginator() {
    return originator;
  }

  public void setOriginator(NotamOriginator originator) {
    this.originator = originator;
  }

  public String getValidTo() {
    return validTo;
  }

  public void setValidTo(String validTo) {
    this.validTo = validTo;
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
