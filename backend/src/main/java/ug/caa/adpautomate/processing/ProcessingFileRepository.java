package ug.caa.adpautomate.processing;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ProcessingFileRepository extends JpaRepository<ProcessingFile, UUID> {
  Optional<ProcessingFile> findByOwnerTypeAndOwnerIdAndRetiredFalse(
      ProcessingOwnerType ownerType,
      UUID ownerId
  );
}
