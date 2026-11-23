package ug.caa.adpautomate.publication;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PublicationRepository extends JpaRepository<Publication, UUID> {

  List<Publication> findByRetiredFalseOrderByEffectiveDescCreatedAtDesc();

  Optional<Publication> findByIdAndRetiredFalse(UUID id);

  long countByRetiredFalseAndStatusNotAndEffectiveGreaterThanEqual(
      PublicationStatus status,
      LocalDate today
  );

  @Query("""
      select p.effective, count(p) from Publication p
      where p.retired = false
        and p.status <> ug.caa.adpautomate.publication.PublicationStatus.CANCELLED
        and p.type in (
          ug.caa.adpautomate.publication.PublicationType.AIRAC_AIP_AMDT,
          ug.caa.adpautomate.publication.PublicationType.AIRAC_AIP_SUP
        )
        and p.effective in :dates
      group by p.effective
      """)
  List<Object[]> countAiracByEffectiveDates(@Param("dates") List<LocalDate> dates);
}
