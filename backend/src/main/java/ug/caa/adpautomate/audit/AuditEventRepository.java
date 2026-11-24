package ug.caa.adpautomate.audit;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

  List<AuditEvent> findByOrderByOccurredAtDesc(Pageable pageable);

  List<AuditEvent> findByEntityTypeAndEntityIdOrderByOccurredAtDesc(
      String entityType,
      String entityId,
      Pageable pageable
  );
}
