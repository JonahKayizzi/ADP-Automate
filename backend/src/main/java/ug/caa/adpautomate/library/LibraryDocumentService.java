package ug.caa.adpautomate.library;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ug.caa.adpautomate.account.AccountRole;
import ug.caa.adpautomate.auth.AccountPrincipal;
import ug.caa.adpautomate.auth.AuthService;
import ug.caa.adpautomate.library.dto.LibraryDtos.LibraryDocumentResponse;
import ug.caa.adpautomate.library.dto.LibraryDtos.UpsertLibraryDocumentRequest;
import ug.caa.adpautomate.web.ApiException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class LibraryDocumentService {

  private final LibraryDocumentRepository documents;

  public LibraryDocumentService(LibraryDocumentRepository documents) {
    this.documents = documents;
  }

  @Transactional(readOnly = true)
  public List<LibraryDocumentResponse> list() {
    return documents.findByRetiredFalseOrderByDocTypeAscTitleAsc().stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public LibraryDocumentResponse get(UUID id) {
    return toResponse(requireActive(id));
  }

  @Transactional(readOnly = true)
  public long countActive() {
    return documents.countByRetiredFalse();
  }

  @Transactional
  public LibraryDocumentResponse create(UpsertLibraryDocumentRequest request) {
    AccountPrincipal actor = requireEditor();
    Instant now = Instant.now();
    LibraryDocument document = new LibraryDocument();
    document.setId(UUID.randomUUID());
    apply(document, request);
    document.setExample(false);
    document.setRetired(false);
    document.setCreatedAt(now);
    document.setCreatedBy(actor.getId());
    document.setCreatedByAccount(actor.getUsername());
    document.setUpdatedAt(now);
    document.setUpdatedByAccount(actor.getUsername());
    documents.save(document);
    return toResponse(document);
  }

  @Transactional
  public LibraryDocumentResponse update(UUID id, UpsertLibraryDocumentRequest request) {
    AccountPrincipal actor = requireEditor();
    LibraryDocument document = requireActive(id);
    apply(document, request);
    document.setExample(false);
    document.setUpdatedAt(Instant.now());
    document.setUpdatedByAccount(actor.getUsername());
    documents.save(document);
    return toResponse(document);
  }

  @Transactional
  public void delete(UUID id) {
    AccountPrincipal actor = requireEditor();
    LibraryDocument document = requireActive(id);
    document.setRetired(true);
    document.setUpdatedAt(Instant.now());
    document.setUpdatedByAccount(actor.getUsername());
    documents.save(document);
  }

  private void apply(LibraryDocument document, UpsertLibraryDocumentRequest request) {
    try {
      document.setDocType(LibraryDocType.fromLabel(request.docType()));
    } catch (IllegalArgumentException ex) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "validation", "Choose a valid document type.", "docType");
    }
    document.setTitle(request.title().trim());
    document.setEdition(blankToNull(request.edition()));
  }

  private LibraryDocument requireActive(UUID id) {
    return documents.findByIdAndRetiredFalse(id)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "not-found", "Library document not found."));
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

  private LibraryDocumentResponse toResponse(LibraryDocument document) {
    return new LibraryDocumentResponse(
        document.getId(),
        document.getDocType().getLabel(),
        document.getTitle(),
        document.getEdition(),
        document.isExample(),
        document.getCreatedAt(),
        document.getCreatedByAccount(),
        document.getUpdatedAt()
    );
  }

  private static String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}
