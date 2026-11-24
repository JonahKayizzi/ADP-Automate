package ug.caa.adpautomate.processing.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class ProcessingDtos {

  private ProcessingDtos() {
  }

  public record SignatureResponse(
      String slotKey,
      String slotLabel,
      String signerName,
      String designation,
      Instant signedAt,
      String signedByAccount,
      boolean signed
  ) {
  }

  public record FormSummary(
      UUID id,
      int stepNumber,
      String formCode,
      String formTitle,
      String status,
      int signedCount,
      int requiredSignatures
  ) {
  }

  public record FormDetail(
      UUID id,
      int stepNumber,
      String formCode,
      String formTitle,
      String controlledNotice,
      String status,
      boolean locked,
      Map<String, Object> payload,
      List<SignatureResponse> signatures,
      Instant updatedAt
  ) {
  }

  public record ProcessingFileResponse(
      UUID id,
      String ownerType,
      UUID ownerId,
      String recordKind,
      int signedOffCount,
      int totalForms,
      List<FormSummary> forms
  ) {
  }

  public record SaveFormRequest(Map<String, Object> payload) {
  }

  public record SignRequest(String signerName, String designation) {
  }

  public record FormRevisionResponse(
      UUID id,
      int revisionNumber,
      String formCode,
      Map<String, Object> payload,
      List<Map<String, Object>> signatures,
      Instant revisedAt,
      String revisedByAccount
  ) {
  }
}
