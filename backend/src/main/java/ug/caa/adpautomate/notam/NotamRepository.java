package ug.caa.adpautomate.notam;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotamRepository extends JpaRepository<Notam, UUID> {
  List<Notam> findByRetiredFalseOrderByIssuedDescCreatedAtDesc();

  Optional<Notam> findByIdAndRetiredFalse(UUID id);

  boolean existsByNumberIgnoreCaseAndRetiredFalse(String number);

  boolean existsByNumberIgnoreCaseAndRetiredFalseAndIdNot(String number, UUID id);

  long countByRetiredFalse();
}
