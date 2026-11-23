package ug.caa.adpautomate.complaint;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ug.caa.adpautomate.account.AccountRole;
import ug.caa.adpautomate.auth.AccountPrincipal;
import ug.caa.adpautomate.auth.AuthService;
import ug.caa.adpautomate.complaint.dto.ComplaintDtos.ComplaintResponse;
import ug.caa.adpautomate.complaint.dto.ComplaintDtos.UpsertComplaintRequest;
import ug.caa.adpautomate.web.ApiException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class ComplaintService {

  private final ComplaintRepository complaints;

  public ComplaintService(ComplaintRepository complaints) {
    this.complaints = complaints;
  }

  @Transactional(readOnly = true)
  public List<ComplaintResponse> list(String statusFilter) {
    List<Complaint> rows;
    if (statusFilter == null || statusFilter.isBlank() || "all".equalsIgnoreCase(statusFilter)) {
      rows = complaints.findByRetiredFalseOrderByReceivedDescCreatedAtDesc();
    } else {
      ComplaintStatus status = parseStatus(statusFilter);
      rows = complaints.findByRetiredFalseAndStatusOrderByReceivedDescCreatedAtDesc(status);
    }
    return rows.stream().map(this::toResponse).toList();
  }

  @Transactional(readOnly = true)
  public long countOpen() {
    return complaints.countByRetiredFalseAndStatus(ComplaintStatus.OPEN);
  }

  @Transactional
  public ComplaintResponse create(UpsertComplaintRequest request) {
    AccountPrincipal actor = requireEditor();
    Instant now = Instant.now();
    Complaint complaint = new Complaint();
    complaint.setId(UUID.randomUUID());
    apply(complaint, request);
    complaint.setExample(false);
    complaint.setRetired(false);
    complaint.setCreatedAt(now);
    complaint.setCreatedBy(actor.getId());
    complaint.setCreatedByAccount(actor.getUsername());
    complaint.setUpdatedAt(now);
    complaint.setUpdatedByAccount(actor.getUsername());
    complaints.save(complaint);
    return toResponse(complaint);
  }

  @Transactional
  public ComplaintResponse update(UUID id, UpsertComplaintRequest request) {
    AccountPrincipal actor = requireEditor();
    Complaint complaint = requireActive(id);
    apply(complaint, request);
    complaint.setExample(false);
    complaint.setUpdatedAt(Instant.now());
    complaint.setUpdatedByAccount(actor.getUsername());
    complaints.save(complaint);
    return toResponse(complaint);
  }

  @Transactional
  public void delete(UUID id) {
    AccountPrincipal actor = requireEditor();
    Complaint complaint = requireActive(id);
    complaint.setRetired(true);
    complaint.setUpdatedAt(Instant.now());
    complaint.setUpdatedByAccount(actor.getUsername());
    complaints.save(complaint);
  }

  private void apply(Complaint complaint, UpsertComplaintRequest request) {
    complaint.setTitle(request.title().trim());
    complaint.setComplainant(request.complainant().trim());
    complaint.setReceived(request.received());
    try {
      complaint.setChannel(ComplaintChannel.fromLabel(request.channel()));
    } catch (IllegalArgumentException ex) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "validation", "Choose a valid channel.", "channel");
    }
    try {
      complaint.setStatus(ComplaintStatus.fromLabel(request.status()));
    } catch (IllegalArgumentException ex) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "validation", "Status must be Open or Closed.", "status");
    }
    complaint.setDescription(blankToNull(request.description()));
    complaint.setActionTaken(blankToNull(request.actionTaken()));
  }

  private Complaint requireActive(UUID id) {
    return complaints.findByIdAndRetiredFalse(id)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "not-found", "Complaint not found."));
  }

  private ComplaintStatus parseStatus(String label) {
    try {
      return ComplaintStatus.fromLabel(label);
    } catch (IllegalArgumentException ex) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "validation", "Filter status must be Open, Closed, or All.", "status");
    }
  }

  private ComplaintResponse toResponse(Complaint complaint) {
    return new ComplaintResponse(
        complaint.getId(),
        complaint.getTitle(),
        complaint.getComplainant(),
        complaint.getReceived(),
        complaint.getChannel().getLabel(),
        complaint.getStatus().getLabel(),
        complaint.getDescription(),
        complaint.getActionTaken(),
        complaint.isExample(),
        complaint.getCreatedAt(),
        complaint.getCreatedByAccount(),
        complaint.getUpdatedAt()
    );
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

  private static String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}
