package ug.caa.adpautomate.publication;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ug.caa.adpautomate.account.AccountRole;
import ug.caa.adpautomate.airac.AiracCalendar;
import ug.caa.adpautomate.airac.AiracController;
import ug.caa.adpautomate.auth.AccountPrincipal;
import ug.caa.adpautomate.auth.AuthService;
import ug.caa.adpautomate.publication.dto.PublicationDtos.AiracOption;
import ug.caa.adpautomate.publication.dto.PublicationDtos.PublicationResponse;
import ug.caa.adpautomate.publication.dto.PublicationDtos.UpsertPublicationRequest;
import ug.caa.adpautomate.web.ApiException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class PublicationService {

  private final PublicationRepository publications;

  public PublicationService(PublicationRepository publications) {
    this.publications = publications;
  }

  @Transactional(readOnly = true)
  public List<PublicationResponse> list() {
    LocalDate today = LocalDate.now(AiracController.UNIT_ZONE);
    return publications.findByRetiredFalseOrderByEffectiveDescCreatedAtDesc().stream()
        .map(p -> toResponse(p, today))
        .toList();
  }

  @Transactional(readOnly = true)
  public PublicationResponse get(UUID id) {
    return toResponse(requireActive(id), LocalDate.now(AiracController.UNIT_ZONE));
  }

  @Transactional(readOnly = true)
  public long countNotYetEffective() {
    LocalDate today = LocalDate.now(AiracController.UNIT_ZONE);
    return publications.countByRetiredFalseAndStatusNotAndEffectiveGreaterThanEqual(
        PublicationStatus.CANCELLED,
        today
    );
  }

  @Transactional(readOnly = true)
  public Map<LocalDate, Long> airacCountsFor(List<LocalDate> dates) {
    Map<LocalDate, Long> map = new HashMap<>();
    if (dates.isEmpty()) {
      return map;
    }
    for (Object[] row : publications.countAiracByEffectiveDates(dates)) {
      map.put((LocalDate) row[0], (Long) row[1]);
    }
    return map;
  }

  @Transactional(readOnly = true)
  public List<AiracOption> airacOptions() {
    LocalDate today = LocalDate.now(AiracController.UNIT_ZONE);
    LocalDate inForce = AiracCalendar.cycleStartContaining(today);
    LocalDate next = inForce.plusDays(AiracCalendar.CYCLE_DAYS);
    List<AiracOption> options = new ArrayList<>();
    // 6 past + current in-force + 14 future from next = start 6 cycles before in-force
    LocalDate start = inForce.minusDays(6L * AiracCalendar.CYCLE_DAYS);
    for (int i = 0; i < 6 + 1 + 14; i++) {
      LocalDate effective = start.plusDays((long) i * AiracCalendar.CYCLE_DAYS);
      LocalDate distributeBy = AiracCalendar.distributeBy(effective);
      boolean isNextDefault = effective.equals(next);
      options.add(new AiracOption(
          AiracCalendar.identForEffectiveDate(effective),
          effective,
          distributeBy,
          ChronoUnit.DAYS.between(today, distributeBy),
          isNextDefault
      ));
    }
    return options;
  }

  @Transactional
  public PublicationResponse create(UpsertPublicationRequest request) {
    AccountPrincipal actor = requireEditor();
    Instant now = Instant.now();
    Publication publication = new Publication();
    publication.setId(UUID.randomUUID());
    apply(publication, request);
    publication.setExample(false);
    publication.setRetired(false);
    publication.setCreatedAt(now);
    publication.setCreatedBy(actor.getId());
    publication.setCreatedByAccount(actor.getUsername());
    publication.setUpdatedAt(now);
    publication.setUpdatedByAccount(actor.getUsername());
    publications.save(publication);
    return toResponse(publication, LocalDate.now(AiracController.UNIT_ZONE));
  }

  @Transactional
  public PublicationResponse update(UUID id, UpsertPublicationRequest request) {
    AccountPrincipal actor = requireEditor();
    Publication publication = requireActive(id);
    apply(publication, request);
    publication.setExample(false);
    publication.setUpdatedAt(Instant.now());
    publication.setUpdatedByAccount(actor.getUsername());
    publications.save(publication);
    return toResponse(publication, LocalDate.now(AiracController.UNIT_ZONE));
  }

  @Transactional
  public void delete(UUID id) {
    AccountPrincipal actor = requireEditor();
    Publication publication = requireActive(id);
    publication.setRetired(true);
    publication.setUpdatedAt(Instant.now());
    publication.setUpdatedByAccount(actor.getUsername());
    publications.save(publication);
  }

  private void apply(Publication publication, UpsertPublicationRequest request) {
    PublicationType type;
    try {
      type = PublicationType.fromLabel(request.type());
    } catch (IllegalArgumentException ex) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "validation", "Choose a valid publication type.", "type");
    }
    PublicationStatus status;
    try {
      status = PublicationStatus.fromLabel(request.status());
    } catch (IllegalArgumentException ex) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "validation", "Choose a valid status.", "status");
    }

    if (type.isAirac() && !AiracCalendar.isAiracDate(request.effective())) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          "validation",
          "AIRAC publications must take an AIRAC effective date.",
          "effective"
      );
    }

    publication.setType(type);
    publication.setNumber(request.number().trim());
    publication.setTitle(request.title().trim());
    publication.setEffective(request.effective());
    publication.setStatus(status);
  }

  private Publication requireActive(UUID id) {
    return publications.findByIdAndRetiredFalse(id)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "not-found", "Publication not found."));
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

  private PublicationResponse toResponse(Publication publication, LocalDate today) {
    boolean airac = publication.getType().isAirac();
    boolean effectiveIsAirac = AiracCalendar.isAiracDate(publication.getEffective());
    String ident = airac && effectiveIsAirac
        ? AiracCalendar.identForEffectiveDate(publication.getEffective())
        : null;
    LocalDate distributeBy = airac ? AiracCalendar.distributeBy(publication.getEffective()) : null;
    Long daysToDeadline = distributeBy == null ? null : ChronoUnit.DAYS.between(today, distributeBy);

    return new PublicationResponse(
        publication.getId(),
        publication.getType().getLabel(),
        publication.getNumber(),
        publication.getTitle(),
        publication.getEffective(),
        publication.getStatus().getLabel(),
        airac,
        ident,
        distributeBy,
        daysToDeadline,
        effectiveIsAirac,
        publication.isExample(),
        publication.getCreatedAt(),
        publication.getCreatedByAccount(),
        publication.getUpdatedAt()
    );
  }
}
