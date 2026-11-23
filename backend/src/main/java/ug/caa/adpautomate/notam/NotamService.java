package ug.caa.adpautomate.notam;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ug.caa.adpautomate.account.AccountRole;
import ug.caa.adpautomate.auth.AccountPrincipal;
import ug.caa.adpautomate.auth.AuthService;
import ug.caa.adpautomate.notam.dto.NotamDtos.NotamResponse;
import ug.caa.adpautomate.notam.dto.NotamDtos.UpsertNotamRequest;
import ug.caa.adpautomate.web.ApiException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
public class NotamService {

  private static final Pattern NUMBER = Pattern.compile("^[A-Z]\\d{4}/\\d{2}$");
  private static final ZoneId UNIT_ZONE = ZoneId.of("Africa/Kampala");

  private final NotamRepository notams;

  public NotamService(NotamRepository notams) {
    this.notams = notams;
  }

  @Transactional(readOnly = true)
  public List<NotamResponse> list() {
    return notams.findByRetiredFalseOrderByIssuedDescCreatedAtDesc().stream()
        .map(this::toResponse)
        .toList();
  }

  @Transactional(readOnly = true)
  public NotamResponse get(UUID id) {
    return toResponse(requireActive(id));
  }

  @Transactional(readOnly = true)
  public long countActive() {
    return notams.countByRetiredFalse();
  }

  @Transactional
  public NotamResponse create(UpsertNotamRequest request) {
    AccountPrincipal actor = requireEditor();
    String number = normalizeNumber(request.number());
    if (notams.existsByNumberIgnoreCaseAndRetiredFalse(number)) {
      throw new ApiException(HttpStatus.CONFLICT, "validation", "A NOTAM with that number already exists.", "number");
    }

    Instant now = Instant.now();
    Notam notam = new Notam();
    notam.setId(UUID.randomUUID());
    apply(notam, request, number);
    notam.setExample(false);
    notam.setRetired(false);
    notam.setCreatedAt(now);
    notam.setCreatedBy(actor.getId());
    notam.setCreatedByAccount(actor.getUsername());
    notam.setUpdatedAt(now);
    notam.setUpdatedByAccount(actor.getUsername());
    notams.save(notam);
    return toResponse(notam);
  }

  @Transactional
  public NotamResponse update(UUID id, UpsertNotamRequest request) {
    AccountPrincipal actor = requireEditor();
    Notam notam = requireActive(id);
    String number = normalizeNumber(request.number());
    if (notams.existsByNumberIgnoreCaseAndRetiredFalseAndIdNot(number, id)) {
      throw new ApiException(HttpStatus.CONFLICT, "validation", "A NOTAM with that number already exists.", "number");
    }
    apply(notam, request, number);
    notam.setExample(false);
    notam.setUpdatedAt(Instant.now());
    notam.setUpdatedByAccount(actor.getUsername());
    notams.save(notam);
    return toResponse(notam);
  }

  @Transactional
  public void delete(UUID id) {
    AccountPrincipal actor = requireEditor();
    Notam notam = requireActive(id);
    notam.setRetired(true);
    notam.setUpdatedAt(Instant.now());
    notam.setUpdatedByAccount(actor.getUsername());
    notams.save(notam);
  }

  private void apply(Notam notam, UpsertNotamRequest request, String number) {
    notam.setNumber(number);
    notam.setIssued(request.issued() != null ? request.issued() : LocalDate.now(UNIT_ZONE));
    notam.setTitle(request.title().trim());
    try {
      notam.setOriginator(NotamOriginator.fromLabel(request.originator()));
    } catch (IllegalArgumentException ex) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "validation", "Choose a valid originator.", "originator");
    }
    notam.setValidTo(blankToNull(request.validTo()));
  }

  private String normalizeNumber(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          "validation",
          "Write the NOTAM number as series letter, four digits, slash, year, for example A0234/26.",
          "number"
      );
    }
    String number = raw.trim().toUpperCase(Locale.ROOT);
    if (!NUMBER.matcher(number).matches()) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          "validation",
          "Write the NOTAM number as series letter, four digits, slash, year, for example A0234/26.",
          "number"
      );
    }
    return number;
  }

  private Notam requireActive(UUID id) {
    return notams.findByIdAndRetiredFalse(id)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "not-found", "NOTAM not found."));
  }

  private AccountPrincipal requireEditor() {
    AccountPrincipal principal = AuthService.currentPrincipal()
        .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "auth", "Not signed in."));
    if (principal.getRole() == AccountRole.VIEWER) {
      throw new ApiException(HttpStatus.FORBIDDEN, "forbidden", "Your account has the Viewer role, so you can read records but not change them.");
    }
    return principal;
  }

  private NotamResponse toResponse(Notam notam) {
    return new NotamResponse(
        notam.getId(),
        notam.getNumber(),
        notam.getIssued(),
        notam.getTitle(),
        notam.getOriginator().getLabel(),
        notam.getValidTo(),
        notam.isExample(),
        notam.getCreatedAt(),
        notam.getCreatedByAccount(),
        notam.getUpdatedAt()
    );
  }

  private static String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}
