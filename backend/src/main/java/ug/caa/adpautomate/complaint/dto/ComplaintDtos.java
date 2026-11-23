package ug.caa.adpautomate.complaint.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class ComplaintDtos {

  private ComplaintDtos() {
  }

  public record ComplaintResponse(
      UUID id,
      String title,
      String complainant,
      LocalDate received,
      String channel,
      String status,
      String description,
      String actionTaken,
      boolean example,
      Instant createdAt,
      String createdByAccount,
      Instant updatedAt
  ) {
  }

  public record UpsertComplaintRequest(
      @NotBlank String title,
      @NotBlank String complainant,
      @NotNull LocalDate received,
      @NotBlank String channel,
      @NotBlank String status,
      String description,
      String actionTaken
  ) {
  }
}
