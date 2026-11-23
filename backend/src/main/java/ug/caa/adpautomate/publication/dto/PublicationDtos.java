package ug.caa.adpautomate.publication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class PublicationDtos {

  private PublicationDtos() {}

  public record PublicationResponse(
      UUID id,
      String type,
      String number,
      String title,
      LocalDate effective,
      String status,
      boolean airac,
      String airacIdent,
      LocalDate distributeBy,
      Long daysToDeadline,
      boolean effectiveIsAiracDate,
      boolean example,
      Instant createdAt,
      String createdByAccount,
      Instant updatedAt
  ) {}

  public record UpsertPublicationRequest(
      @NotBlank String type,
      @NotBlank String number,
      @NotBlank String title,
      @NotNull LocalDate effective,
      String status
  ) {}

  public record AiracOption(
      String ident,
      LocalDate effective,
      LocalDate distributeBy,
      long daysToDeadline,
      boolean nextDefault
  ) {}
}
