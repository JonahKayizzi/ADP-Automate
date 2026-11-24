package ug.caa.adpautomate.processing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProcessingFormRevisionRepository extends JpaRepository<ProcessingFormRevision, UUID> {

  List<ProcessingFormRevision> findByFormIdOrderByRevisionNumberDesc(UUID formId);

  Optional<ProcessingFormRevision> findFirstByFormIdOrderByRevisionNumberDesc(UUID formId);
}
