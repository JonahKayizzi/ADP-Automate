package ug.caa.adpautomate.attachment;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import ug.caa.adpautomate.account.AccountRole;
import ug.caa.adpautomate.attachment.dto.AttachmentDtos.AttachmentResponse;
import ug.caa.adpautomate.audit.AuditService;
import ug.caa.adpautomate.auth.AccountPrincipal;
import ug.caa.adpautomate.auth.AuthService;
import ug.caa.adpautomate.library.LibraryDocumentRepository;
import ug.caa.adpautomate.notam.NotamRepository;
import ug.caa.adpautomate.publication.PublicationRepository;
import ug.caa.adpautomate.web.ApiException;

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
public class AttachmentService {

  private static final long MAX_BYTES = 20L * 1024 * 1024;
  private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
      "pdf", "txt", "csv", "json", "xml", "gml", "md", "png", "jpg", "jpeg"
  );
  private static final Map<String, String> DEFAULT_CONTENT_TYPES = Map.ofEntries(
      Map.entry("pdf", "application/pdf"),
      Map.entry("txt", "text/plain"),
      Map.entry("csv", "text/csv"),
      Map.entry("json", "application/json"),
      Map.entry("xml", "application/xml"),
      Map.entry("gml", "application/gml+xml"),
      Map.entry("md", "text/markdown"),
      Map.entry("png", "image/png"),
      Map.entry("jpg", "image/jpeg"),
      Map.entry("jpeg", "image/jpeg")
  );

  private final AttachmentRepository attachments;
  private final LocalFileStorage storage;
  private final NotamRepository notams;
  private final PublicationRepository publications;
  private final LibraryDocumentRepository libraryDocuments;
  private final AuditService auditService;

  public AttachmentService(
      AttachmentRepository attachments,
      LocalFileStorage storage,
      NotamRepository notams,
      PublicationRepository publications,
      LibraryDocumentRepository libraryDocuments,
      AuditService auditService
  ) {
    this.attachments = attachments;
    this.storage = storage;
    this.notams = notams;
    this.publications = publications;
    this.libraryDocuments = libraryDocuments;
    this.auditService = auditService;
  }

  @Transactional(readOnly = true)
  public List<AttachmentResponse> list(String ownerTypeLabel, UUID ownerId) {
    AttachmentOwnerType ownerType = parseOwnerType(ownerTypeLabel);
    requireOwnerExists(ownerType, ownerId);
    return attachments.findByOwnerTypeAndOwnerIdAndRetiredFalseOrderByCreatedAtDesc(ownerType, ownerId).stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional
  public AttachmentResponse upload(String ownerTypeLabel, UUID ownerId, String slotLabel, MultipartFile file) {
    AccountPrincipal actor = requireEditor();
    AttachmentOwnerType ownerType = parseOwnerType(ownerTypeLabel);
    requireOwnerExists(ownerType, ownerId);
    String slot = normalizeSlot(ownerType, slotLabel);

    if (file == null || file.isEmpty()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "validation", "Choose a file to upload.", "file");
    }
    if (file.getSize() > MAX_BYTES) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "validation", "Each file must be 20 MB or smaller.", "file");
    }

    String original = sanitizeFilename(file.getOriginalFilename());
    String extension = extensionOf(original);
    if (!ALLOWED_EXTENSIONS.contains(extension)) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          "validation",
          "Allowed types: PDF, TXT, CSV, JSON, XML, GML, MD, PNG, JPEG.",
          "file"
      );
    }

    Instant now = Instant.now();
    if (slot != null) {
      retireSlot(ownerType, ownerId, slot, actor, now);
    }

    UUID id = UUID.randomUUID();
    String storageKey = ownerType.getLabel() + "/" + ownerId + "/" + id + "." + extension;
    String sha256;
    try (InputStream in = file.getInputStream()) {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      try (DigestInputStream digester = new DigestInputStream(in, digest)) {
        storage.store(storageKey, digester);
      }
      sha256 = HexFormat.of().formatHex(digest.digest());
    } catch (IOException | NoSuchAlgorithmException ex) {
      storage.deleteQuietly(storageKey);
      throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "storage", "Could not store the uploaded file.");
    }

    Attachment attachment = new Attachment();
    attachment.setId(id);
    attachment.setOwnerType(ownerType);
    attachment.setOwnerId(ownerId);
    attachment.setSlot(slot);
    attachment.setOriginalFilename(original);
    attachment.setContentType(resolveContentType(file.getContentType(), extension));
    attachment.setSizeBytes(file.getSize());
    attachment.setStorageKey(storageKey);
    attachment.setSha256(sha256);
    attachment.setRetired(false);
    attachment.setCreatedAt(now);
    attachment.setCreatedBy(actor.getId());
    attachment.setCreatedByAccount(actor.getUsername());
    attachment.setUpdatedAt(now);
    attachment.setUpdatedByAccount(actor.getUsername());
    attachments.save(attachment);
    auditService.record(
        "attachment.upload",
        "attachment",
        attachment.getId().toString(),
        Map.of(
            "ownerType", ownerType.getLabel(),
            "ownerId", ownerId.toString(),
            "slot", slot == null ? "" : slot,
            "filename", original
        )
    );
    return toResponse(attachment);
  }

  @Transactional(readOnly = true)
  public List<AttachmentResponse> listHistory(String ownerTypeLabel, UUID ownerId) {
    AttachmentOwnerType ownerType = parseOwnerType(ownerTypeLabel);
    requireOwnerExists(ownerType, ownerId);
    return attachments.findByOwnerTypeAndOwnerIdAndRetiredTrueOrderByUpdatedAtDesc(ownerType, ownerId)
        .stream()
        .map(this::toResponse)
        .toList();
  }

  private void retireSlot(
      AttachmentOwnerType ownerType,
      UUID ownerId,
      String slot,
      AccountPrincipal actor,
      Instant now
  ) {
    List<Attachment> existing =
        attachments.findByOwnerTypeAndOwnerIdAndSlotAndRetiredFalse(ownerType, ownerId, slot);
    for (Attachment previous : existing) {
      previous.setRetired(true);
      previous.setUpdatedAt(now);
      previous.setUpdatedByAccount(actor.getUsername());
    }
    if (!existing.isEmpty()) {
      attachments.saveAll(existing);
    }
  }

  /**
   * NOTAM: supporting | product. Publication: publication. Library: null (multi-attach).
   */
  static String normalizeSlot(AttachmentOwnerType ownerType, String slotLabel) {
    String slot = slotLabel == null || slotLabel.isBlank() ? null : slotLabel.trim().toLowerCase(Locale.ROOT);
    return switch (ownerType) {
      case LIBRARY -> null;
      case NOTAM -> {
        if (slot == null) {
          throw new ApiException(
              HttpStatus.BAD_REQUEST,
              "validation",
              "Choose a slot: supporting or product.",
              "slot"
          );
        }
        if (!"supporting".equals(slot) && !"product".equals(slot)) {
          throw new ApiException(
              HttpStatus.BAD_REQUEST,
              "validation",
              "NOTAM slot must be supporting or product.",
              "slot"
          );
        }
        yield slot;
      }
      case PUBLICATION -> {
        if (slot == null || "publication".equals(slot) || "file".equals(slot)) {
          yield "publication";
        }
        throw new ApiException(
            HttpStatus.BAD_REQUEST,
            "validation",
            "Publication slot must be publication.",
            "slot"
        );
      }
    };
  }

  @Transactional(readOnly = true)
  public LoadedAttachment download(UUID id) {
    Attachment attachment = requireActive(id);
    Resource resource = storage.load(attachment.getStorageKey());
    return new LoadedAttachment(attachment, resource);
  }

  @Transactional
  public void delete(UUID id) {
    AccountPrincipal actor = requireEditor();
    Attachment attachment = requireActive(id);
    attachment.setRetired(true);
    attachment.setUpdatedAt(Instant.now());
    attachment.setUpdatedByAccount(actor.getUsername());
    attachments.save(attachment);
    auditService.record(
        "attachment.remove",
        "attachment",
        attachment.getId().toString(),
        Map.of(
            "ownerType", attachment.getOwnerType().getLabel(),
            "ownerId", attachment.getOwnerId().toString(),
            "filename", attachment.getOriginalFilename()
        )
    );
  }

  @Transactional(readOnly = true)
  public LoadedAttachment downloadAny(UUID id) {
    Attachment attachment = attachments.findById(id)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "not-found", "Attachment not found."));
    Resource resource = storage.load(attachment.getStorageKey());
    return new LoadedAttachment(attachment, resource);
  }

  private void requireOwnerExists(AttachmentOwnerType ownerType, UUID ownerId) {
    boolean exists = switch (ownerType) {
      case NOTAM -> notams.findByIdAndRetiredFalse(ownerId).isPresent();
      case PUBLICATION -> publications.findByIdAndRetiredFalse(ownerId).isPresent();
      case LIBRARY -> libraryDocuments.findByIdAndRetiredFalse(ownerId).isPresent();
    };
    if (!exists) {
      throw new ApiException(HttpStatus.NOT_FOUND, "not-found", "Parent record not found.");
    }
  }

  private Attachment requireActive(UUID id) {
    return attachments.findByIdAndRetiredFalse(id)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "not-found", "Attachment not found."));
  }

  private AccountPrincipal requireEditor() {
    AccountPrincipal principal = AuthService.currentPrincipal()
        .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "auth", "Not signed in."));
    if (principal.getRole() == AccountRole.VIEWER) {
      throw new ApiException(
          HttpStatus.FORBIDDEN,
          "forbidden",
          "Your account has the Viewer role, so you can read records but not change them."
      );
    }
    return principal;
  }

  private AttachmentOwnerType parseOwnerType(String label) {
    try {
      return AttachmentOwnerType.fromLabel(label);
    } catch (IllegalArgumentException ex) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "validation", "Owner type must be notam, publication, or library.", "ownerType");
    }
  }

  private AttachmentResponse toResponse(Attachment attachment) {
    return new AttachmentResponse(
        attachment.getId(),
        attachment.getOwnerType().getLabel(),
        attachment.getOwnerId(),
        attachment.getSlot(),
        attachment.getOriginalFilename(),
        attachment.getContentType(),
        attachment.getSizeBytes(),
        attachment.getSha256(),
        attachment.getCreatedAt(),
        attachment.getCreatedByAccount()
    );
  }

  private static String sanitizeFilename(String name) {
    if (name == null || name.isBlank()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "validation", "The file must have a name.", "file");
    }
    String trimmed = name.replace('\\', '/');
    int slash = trimmed.lastIndexOf('/');
    if (slash >= 0) {
      trimmed = trimmed.substring(slash + 1);
    }
    trimmed = trimmed.trim();
    if (trimmed.isEmpty() || trimmed.equals(".") || trimmed.equals("..")) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "validation", "The file must have a valid name.", "file");
    }
    if (trimmed.length() > 500) {
      trimmed = trimmed.substring(trimmed.length() - 500);
    }
    return trimmed;
  }

  private static String extensionOf(String filename) {
    int dot = filename.lastIndexOf('.');
    if (dot < 0 || dot == filename.length() - 1) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "validation", "The file must include an extension.", "file");
    }
    return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
  }

  private static String resolveContentType(String provided, String extension) {
    if (provided != null && !provided.isBlank() && !"application/octet-stream".equalsIgnoreCase(provided)) {
      return provided.length() > 120 ? provided.substring(0, 120) : provided;
    }
    return DEFAULT_CONTENT_TYPES.getOrDefault(extension, "application/octet-stream");
  }

  public record LoadedAttachment(Attachment attachment, Resource resource) {
  }
}
