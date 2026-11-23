package ug.caa.adpautomate.processing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProcessingFormRepository extends JpaRepository<ProcessingForm, UUID> {
  List<ProcessingForm> findByProcessingFileIdOrderByStepNumberAsc(UUID processingFileId);

  Optional<ProcessingForm> findByIdAndProcessingFileId(UUID id, UUID processingFileId);

  long countByProcessingFileIdAndStatus(UUID processingFileId, ProcessingFormStatus status);
}
