package ug.caa.adpautomate.audit;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ug.caa.adpautomate.auth.AccountPrincipal;
import ug.caa.adpautomate.auth.AuthService;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AuditService {

  private final AuditEventRepository events;

  public AuditService(AuditEventRepository events) {
    this.events = events;
  }

  @Transactional
  public void record(String action, String entityType, String entityId, Map<String, Object> detail) {
    AccountPrincipal actor = AuthService.currentPrincipal().orElse(null);
    AuditEvent event = new AuditEvent();
    event.setId(UUID.randomUUID());
    event.setOccurredAt(Instant.now());
    if (actor != null) {
      event.setActorUsername(actor.getUsername());
      event.setActorUserId(actor.getId());
    }
    event.setAction(action);
    event.setEntityType(entityType);
    event.setEntityId(entityId);
    event.setDetail(detail == null ? new LinkedHashMap<>() : new LinkedHashMap<>(detail));
    events.save(event);
  }

  @Transactional(readOnly = true)
  public List<AuditEventResponse> list(String entityType, String entityId, int limit) {
    int size = Math.min(Math.max(limit, 1), 200);
    List<AuditEvent> rows;
    if (entityType != null && !entityType.isBlank() && entityId != null && !entityId.isBlank()) {
      rows = events.findByEntityTypeAndEntityIdOrderByOccurredAtDesc(
          entityType.trim(),
          entityId.trim(),
          PageRequest.of(0, size)
      );
    } else {
      rows = events.findByOrderByOccurredAtDesc(PageRequest.of(0, size));
    }
    return rows.stream().map(this::toResponse).toList();
  }

  private AuditEventResponse toResponse(AuditEvent event) {
    return new AuditEventResponse(
        event.getId(),
        event.getOccurredAt(),
        event.getActorUsername(),
        event.getAction(),
        event.getEntityType(),
        event.getEntityId(),
        event.getDetail() == null ? Map.of() : event.getDetail()
    );
  }

  public record AuditEventResponse(
      UUID id,
      Instant occurredAt,
      String actorUsername,
      String action,
      String entityType,
      String entityId,
      Map<String, Object> detail
  ) {
  }
}
