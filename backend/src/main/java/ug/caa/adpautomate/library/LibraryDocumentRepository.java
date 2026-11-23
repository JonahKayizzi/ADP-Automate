package ug.caa.adpautomate.library;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LibraryDocumentRepository extends JpaRepository<LibraryDocument, UUID> {
  List<LibraryDocument> findByRetiredFalseOrderByDocTypeAscTitleAsc();

  Optional<LibraryDocument> findByIdAndRetiredFalse(UUID id);

  long countByRetiredFalse();
}
