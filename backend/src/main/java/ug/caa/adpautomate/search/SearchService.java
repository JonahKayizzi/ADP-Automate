package ug.caa.adpautomate.search;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ug.caa.adpautomate.complaint.ComplaintRepository;
import ug.caa.adpautomate.library.LibraryDocumentRepository;
import ug.caa.adpautomate.notam.NotamRepository;
import ug.caa.adpautomate.publication.PublicationRepository;
import ug.caa.adpautomate.task.TaskRepository;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class SearchService {

  /** Max hits returned per module group. */
  static final int LIMIT = 12;

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

    List<String> terms = tokenize(q);

    groups.put(
        "notams",
        notams.findByRetiredFalseOrderByIssuedDescCreatedAtDesc().stream()
            .filter(n -> matches(terms, n.getNumber(), n.getTitle(), n.getOriginator().getLabel()))
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
            .filter(p -> matches(terms, p.getNumber(), p.getTitle(), p.getType().getLabel(), p.getStatus().getLabel()))
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
            .filter(d -> matches(terms, d.getTitle(), d.getEdition(), d.getDocType().getLabel()))
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
                terms,
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
            .filter(t -> matches(terms, t.getTitle(), t.getAssigneeName(), t.getStatus().getLabel()))
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

  /**
   * Case-insensitive; every space-separated term must appear in the record's text fields
   * (ids and secrets are never passed here).
   */
  static boolean matches(List<String> terms, String... fields) {
    if (terms == null || terms.isEmpty()) {
      return false;
    }
    String haystack = Stream.of(fields)
        .filter(field -> field != null && !field.isBlank())
        .map(field -> field.toLowerCase(Locale.ROOT))
        .collect(Collectors.joining(" "));
    if (haystack.isBlank()) {
      return false;
    }
    return terms.stream().allMatch(haystack::contains);
  }

  static List<String> tokenize(String query) {
    if (query == null || query.isBlank()) {
      return List.of();
    }
    return Arrays.stream(query.toLowerCase(Locale.ROOT).trim().split("\\s+"))
        .filter(t -> !t.isBlank())
        .toList();
  }

  public record Hit(UUID id, String type, String title, String subtitle, String href) {
  }
}
