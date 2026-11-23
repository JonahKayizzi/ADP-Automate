package ug.caa.adpautomate.complaint;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ComplaintRepository extends JpaRepository<Complaint, UUID> {
  List<Complaint> findByRetiredFalseOrderByReceivedDescCreatedAtDesc();

  List<Complaint> findByRetiredFalseAndStatusOrderByReceivedDescCreatedAtDesc(ComplaintStatus status);

  Optional<Complaint> findByIdAndRetiredFalse(UUID id);

  long countByRetiredFalseAndStatus(ComplaintStatus status);
}
