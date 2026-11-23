package ug.caa.adpautomate.library.dto;

import jakarta.validation.constraints.NotBlank;

import java.time.Instant;
import java.util.UUID;

public final class LibraryDtos {

  private LibraryDtos() {}

  public record LibraryDocumentResponse(
      UUID id,
      String docType,
      String title,
      String edition,
      boolean example,
      Instant createdAt,
      String createdByAccount,
      Instant updatedAt
  ) {}

  public record UpsertLibraryDocumentRequest(
      @NotBlank String docType,
      @NotBlank String title,
      String edition
  ) {}
}
