package ug.caa.adpautomate.notam.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class NotamDtos {

  private NotamDtos() {}

  public record NotamResponse(
      UUID id,
      String number,
      LocalDate issued,
      String title,
      String originator,
      String validTo,
      boolean example,
      Instant createdAt,
      String createdByAccount,
      Instant updatedAt
  ) {}

  public record UpsertNotamRequest(
      @NotBlank String number,
      @NotNull LocalDate issued,
      @NotBlank String title,
      @NotBlank String originator,
      String validTo
  ) {}
}
