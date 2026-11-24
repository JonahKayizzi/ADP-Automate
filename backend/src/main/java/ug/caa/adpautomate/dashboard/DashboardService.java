package ug.caa.adpautomate.dashboard;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ug.caa.adpautomate.airac.AiracCalendar;
import ug.caa.adpautomate.airac.AiracController;
import ug.caa.adpautomate.auth.AccountPrincipal;
import ug.caa.adpautomate.auth.AuthService;
import ug.caa.adpautomate.complaint.Complaint;
import ug.caa.adpautomate.complaint.ComplaintRepository;
import ug.caa.adpautomate.complaint.ComplaintStatus;
import ug.caa.adpautomate.notam.Notam;
import ug.caa.adpautomate.notam.NotamRepository;
import ug.caa.adpautomate.processing.ProcessingFile;
import ug.caa.adpautomate.processing.ProcessingFileRepository;
import ug.caa.adpautomate.processing.ProcessingFormRepository;
import ug.caa.adpautomate.processing.ProcessingFormStatus;
import ug.caa.adpautomate.processing.ProcessingOwnerType;
import ug.caa.adpautomate.publication.Publication;
import ug.caa.adpautomate.publication.PublicationRepository;
import ug.caa.adpautomate.publication.PublicationStatus;
import ug.caa.adpautomate.task.Task;
import ug.caa.adpautomate.task.TaskRepository;
import ug.caa.adpautomate.task.TaskStatus;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class DashboardService {

  private final NotamRepository notams;
  private final PublicationRepository publications;
  private final ComplaintRepository complaints;
  private final TaskRepository tasks;
  private final ProcessingFileRepository processingFiles;
  private final ProcessingFormRepository processingForms;
  private final int complaintOpenDays;
  private final int airacDraftWarningDays;

  public DashboardService(
      NotamRepository notams,
      PublicationRepository publications,
      ComplaintRepository complaints,
      TaskRepository tasks,
      ProcessingFileRepository processingFiles,
      ProcessingFormRepository processingForms,
      @Value("${app.complaint-open-days:14}") int complaintOpenDays,
      @Value("${app.airac-draft-warning-days:14}") int airacDraftWarningDays
  ) {
    this.notams = notams;
    this.publications = publications;
    this.complaints = complaints;
    this.tasks = tasks;
    this.processingFiles = processingFiles;
    this.processingForms = processingForms;
    this.complaintOpenDays = complaintOpenDays;
    this.airacDraftWarningDays = airacDraftWarningDays;
  }

  @Transactional(readOnly = true)
  public List<Map<String, Object>> needsAttention() {
    LocalDate today = LocalDate.now(AiracController.UNIT_ZONE);
    AccountPrincipal me = AuthService.currentPrincipal().orElse(null);
    List<Map<String, Object>> rows = new ArrayList<>();

    for (Task task : tasks.findByRetiredFalse()) {
      if (task.getStatus() == TaskStatus.DONE) {
        continue;
      }
      if (me != null
          && me.getUsername().equalsIgnoreCase(task.getAssigneeUsername())
          && task.getSeenAt() == null) {
        rows.add(item(
            "critical",
            "task",
            "New task assigned to you"
                + (task.getAssignedByName() == null ? "" : " by " + task.getAssignedByName()),
            task.getTitle(),
            "/tasks"
        ));
      }
      if (task.getDueDate().isBefore(today)) {
        long days = ChronoUnit.DAYS.between(task.getDueDate(), today);
        rows.add(item(
            "critical",
            "task",
            "Overdue by " + days + " day" + (days == 1 ? "" : "s"),
            task.getTitle(),
            "/tasks"
        ));
      }
    }

    for (Publication publication : publications.findByRetiredFalseOrderByEffectiveDescCreatedAtDesc()) {
      if (publication.getStatus() != PublicationStatus.DRAFT || !publication.getType().isAirac()) {
        continue;
      }
      LocalDate deadline = AiracCalendar.distributeBy(publication.getEffective());
      long days = ChronoUnit.DAYS.between(today, deadline);
      if (days <= airacDraftWarningDays) {
        String severity = days < 0 ? "critical" : "warning";
        String detail = days < 0
            ? "AIRAC draft past distribution deadline"
            : "AIRAC draft due in " + days + " day" + (days == 1 ? "" : "s");
        rows.add(item(
            severity,
            "publication",
            detail,
            publication.getType().getLabel() + " " + publication.getNumber(),
            "/publications"
        ));
      }
    }

    for (Notam notam : notams.findByRetiredFalseOrderByIssuedDescCreatedAtDesc()) {
      if (!isFullySignedOff(ProcessingOwnerType.NOTAM, notam.getId())) {
        rows.add(item(
            "warning",
            "notam",
            "Processing file not signed off",
            notam.getNumber() + " — " + notam.getTitle(),
            "/notams"
        ));
      }
    }

    for (Publication publication : publications.findByRetiredFalseOrderByEffectiveDescCreatedAtDesc()) {
      if (publication.getStatus() == PublicationStatus.CANCELLED) {
        continue;
      }
      if (!isFullySignedOff(ProcessingOwnerType.PUBLICATION, publication.getId())) {
        boolean critical = publication.getStatus() == PublicationStatus.DISTRIBUTED;
        rows.add(item(
            critical ? "critical" : "warning",
            "publication",
            "Processing file not signed off",
            publication.getType().getLabel() + " " + publication.getNumber(),
            "/publications"
        ));
      }
    }

    for (Complaint complaint : complaints.findByRetiredFalseAndStatusOrderByReceivedDescCreatedAtDesc(ComplaintStatus.OPEN)) {
      long openDays = ChronoUnit.DAYS.between(complaint.getReceived(), today);
      if (openDays > complaintOpenDays) {
        rows.add(item(
            "warning",
            "complaint",
            "Open for " + openDays + " days",
            complaint.getTitle(),
            "/complaints"
        ));
      }
    }

    rows.sort(Comparator
        .comparing((Map<String, Object> r) -> !"critical".equals(r.get("severity")))
        .thenComparing(r -> String.valueOf(r.get("title"))));

    if (rows.size() > 10) {
      return rows.subList(0, 10);
    }
    return rows;
  }

  private boolean isFullySignedOff(ProcessingOwnerType ownerType, java.util.UUID ownerId) {
    Optional<ProcessingFile> file = processingFiles.findByOwnerTypeAndOwnerIdAndRetiredFalse(ownerType, ownerId);
    return file
        .map(f -> processingForms.countByProcessingFileIdAndStatus(f.getId(), ProcessingFormStatus.SIGNED_OFF) == 4)
        .orElse(false);
  }

  private static Map<String, Object> item(
      String severity,
      String type,
      String detail,
      String title,
      String href
  ) {
    Map<String, Object> row = new LinkedHashMap<>();
    row.put("severity", severity);
    row.put("type", type);
    row.put("detail", detail);
    row.put("title", title);
    row.put("href", href);
    return row;
  }
}
