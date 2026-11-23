package ug.caa.adpautomate.search;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ug.caa.adpautomate.complaint.ComplaintRepository;
import ug.caa.adpautomate.library.LibraryDocumentRepository;
import ug.caa.adpautomate.notam.NotamRepository;
import ug.caa.adpautomate.publication.PublicationRepository;
import ug.caa.adpautomate.task.TaskRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

@Service
public class SearchService {

  private static final int LIMIT = 12;

  private final NotamRepository notams;
  private final PublicationRepository publications;
  private final LibraryDocumentRepository libraryDocuments;
  private final ComplaintRepository complaints;
  private final TaskRepository tasks;

  public SearchService(
      NotamRepository notams,
      PublicationRepository publications,
      LibraryDocumentRepository libraryDocuments,
      ComplaintRepository complaints,
      TaskRepository tasks
  ) {
    this.notams = notams;
    this.publications = publications;
    this.libraryDocuments = libraryDocuments;
    this.complaints = complaints;
    this.tasks = tasks;
  }

  @Transactional(readOnly = true)
  public Map<String, List<Hit>> search(String rawQuery) {
    String q = rawQuery == null ? "" : rawQuery.trim();
    Map<String, List<Hit>> groups = new LinkedHashMap<>();
    groups.put("notams", List.of());
    groups.put("publications", List.of());
    groups.put("library", List.of());
    groups.put("complaints", List.of());
    groups.put("tasks", List.of());
    if (q.length() < 2) {
      return groups;
    }

    String needle = q.toLowerCase(Locale.ROOT);

    groups.put(
        "notams",
        notams.findByRetiredFalseOrderByIssuedDescCreatedAtDesc().stream()
            .filter(n -> matches(needle, n.getNumber(), n.getTitle(), n.getOriginator().getLabel()))
            .limit(LIMIT)
            .map(n -> new Hit(
                n.getId(),
                "notam",
                n.getNumber() + " — " + n.getTitle(),
                n.getOriginator().getLabel(),
                "/notams"
            ))
            .toList()
    );

    groups.put(
        "publications",
        publications.findByRetiredFalseOrderByEffectiveDescCreatedAtDesc().stream()
            .filter(p -> matches(needle, p.getNumber(), p.getTitle(), p.getType().getLabel(), p.getStatus().getLabel()))
            .limit(LIMIT)
            .map(p -> new Hit(
                p.getId(),
                "publication",
                p.getType().getLabel() + " " + p.getNumber(),
                p.getTitle(),
                "/publications"
            ))
            .toList()
    );

    groups.put(
        "library",
        libraryDocuments.findByRetiredFalseOrderByDocTypeAscTitleAsc().stream()
            .filter(d -> matches(needle, d.getTitle(), d.getEdition(), d.getDocType().getLabel()))
            .limit(LIMIT)
            .map(d -> new Hit(
                d.getId(),
                "library",
                d.getTitle(),
                d.getDocType().getLabel() + (d.getEdition() == null ? "" : " · " + d.getEdition()),
                "/library"
            ))
            .toList()
    );

    groups.put(
        "complaints",
        complaints.findByRetiredFalseOrderByReceivedDescCreatedAtDesc().stream()
            .filter(c -> matches(
                needle,
                c.getTitle(),
                c.getComplainant(),
                c.getChannel().getLabel(),
                c.getStatus().getLabel(),
                c.getDescription(),
                c.getActionTaken()
            ))
            .limit(LIMIT)
            .map(c -> new Hit(
                c.getId(),
                "complaint",
                c.getTitle(),
                c.getComplainant() + " · " + c.getStatus().getLabel(),
                "/complaints"
            ))
            .toList()
    );

    groups.put(
        "tasks",
        tasks.findByRetiredFalse().stream()
            .filter(t -> matches(needle, t.getTitle(), t.getAssigneeName(), t.getStatus().getLabel()))
            .limit(LIMIT)
            .map(t -> new Hit(
                t.getId(),
                "task",
                t.getTitle(),
                (t.getAssigneeName() == null ? "" : t.getAssigneeName() + " · ") + t.getStatus().getLabel(),
                "/tasks"
            ))
            .toList()
    );

    return groups;
  }

  private static boolean matches(String needle, String... fields) {
    return Stream.of(fields)
        .filter(field -> field != null && !field.isBlank())
        .anyMatch(field -> field.toLowerCase(Locale.ROOT).contains(needle));
  }

  public record Hit(UUID id, String type, String title, String subtitle, String href) {
  }
}
