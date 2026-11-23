package ug.caa.adpautomate.attachment;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AttachmentRepository extends JpaRepository<Attachment, UUID> {
  List<Attachment> findByOwnerTypeAndOwnerIdAndRetiredFalseOrderByCreatedAtDesc(
      AttachmentOwnerType ownerType,
      UUID ownerId
  );

  Optional<Attachment> findByIdAndRetiredFalse(UUID id);

  long countByOwnerTypeAndOwnerIdAndRetiredFalse(AttachmentOwnerType ownerType, UUID ownerId);
}
