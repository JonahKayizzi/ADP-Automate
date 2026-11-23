package ug.caa.adpautomate.processing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProcessingSignatureRepository extends JpaRepository<ProcessingSignature, UUID> {
  List<ProcessingSignature> findByFormIdOrderBySlotKeyAsc(UUID formId);

  Optional<ProcessingSignature> findByFormIdAndSlotKey(UUID formId, String slotKey);

  List<ProcessingSignature> findByFormIdIn(List<UUID> formIds);
}
