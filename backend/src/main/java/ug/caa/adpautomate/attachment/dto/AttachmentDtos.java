package ug.caa.adpautomate.attachment.dto;

import java.time.Instant;
import java.util.UUID;

public final class AttachmentDtos {

  private AttachmentDtos() {
  }

  public record AttachmentResponse(
      UUID id,
      String ownerType,
      UUID ownerId,
      String slot,
      String originalFilename,
      String contentType,
      long sizeBytes,
      String sha256,
      Instant createdAt,
      String createdByAccount
  ) {
  }
}
