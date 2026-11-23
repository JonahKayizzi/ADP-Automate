package ug.caa.adpautomate.task;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ug.caa.adpautomate.account.Account;
import ug.caa.adpautomate.account.AccountRepository;
import ug.caa.adpautomate.account.AccountRole;
import ug.caa.adpautomate.airac.AiracController;
import ug.caa.adpautomate.auth.AccountPrincipal;
import ug.caa.adpautomate.auth.AuthService;
import ug.caa.adpautomate.task.dto.TaskDtos.AssigneeOption;
import ug.caa.adpautomate.task.dto.TaskDtos.NotifyRequest;
import ug.caa.adpautomate.task.dto.TaskDtos.TaskResponse;
import ug.caa.adpautomate.task.dto.TaskDtos.UpsertTaskRequest;
import ug.caa.adpautomate.web.ApiException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
public class TaskService {

  private final TaskRepository tasks;
  private final AccountRepository accounts;

  public TaskService(TaskRepository tasks, AccountRepository accounts) {
    this.tasks = tasks;
    this.accounts = accounts;
  }

  @Transactional(readOnly = true)
  public List<AssigneeOption> assignees() {
    return accounts.findByActiveTrueOrderByDisplayNameAsc().stream()
        .map(a -> new AssigneeOption(a.getUsername(), a.getDisplayName(), a.getDesignation(), a.getEmail()))
        .toList();
  }

  @Transactional(readOnly = true)
  public List<TaskResponse> list(String filter) {
    AccountPrincipal me = AuthService.currentPrincipal().orElse(null);
    LocalDate today = LocalDate.now(AiracController.UNIT_ZONE);
    String f = filter == null || filter.isBlank() ? "Open" : filter;
    return tasks.findByRetiredFalse().stream()
        .filter(task -> matchesFilter(task, f, me, today))
        .sorted(taskComparator(today))
        .map(task -> toResponse(task, me, today))
        .toList();
  }

  @Transactional(readOnly = true)
  public long countOverdue() {
    LocalDate today = LocalDate.now(AiracController.UNIT_ZONE);
    return tasks.countByRetiredFalseAndStatusNotAndDueDateBefore(TaskStatus.DONE, today);
  }

  @Transactional
  public TaskResponse create(UpsertTaskRequest request) {
    AccountPrincipal actor = requireEditor();
    Instant now = Instant.now();
    Task task = new Task();
    task.setId(UUID.randomUUID());
    task.setExample(false);
    task.setRetired(false);
    task.setCreatedAt(now);
    task.setCreatedBy(actor.getId());
    task.setCreatedByAccount(actor.getUsername());
    apply(task, request, actor, now, true);
    task.setUpdatedAt(now);
    task.setUpdatedByAccount(actor.getUsername());
    tasks.save(task);
    return toResponse(task, actor, LocalDate.now(AiracController.UNIT_ZONE));
  }

  @Transactional
  public TaskResponse update(UUID id, UpsertTaskRequest request) {
    AccountPrincipal actor = requireEditor();
    Task task = requireActive(id);
    Instant now = Instant.now();
    apply(task, request, actor, now, false);
    task.setExample(false);
    task.setUpdatedAt(now);
    task.setUpdatedByAccount(actor.getUsername());
    tasks.save(task);
    return toResponse(task, actor, LocalDate.now(AiracController.UNIT_ZONE));
  }

  @Transactional
  public TaskResponse complete(UUID id) {
    AccountPrincipal actor = requireEditor();
    Task task = requireActive(id);
    if (task.getStatus() == TaskStatus.DONE) {
      return toResponse(task, actor, LocalDate.now(AiracController.UNIT_ZONE));
    }
    Instant now = Instant.now();
    task.setStatus(TaskStatus.DONE);
    task.setCompletedAt(now);
    task.setUpdatedAt(now);
    task.setUpdatedByAccount(actor.getUsername());
    tasks.save(task);
    return toResponse(task, actor, LocalDate.now(AiracController.UNIT_ZONE));
  }

  @Transactional
  public TaskResponse notify(UUID id, NotifyRequest request) {
    AccountPrincipal actor = requireEditor();
    Task task = requireActive(id);
    NotifyStatus outcome;
    try {
      outcome = NotifyStatus.fromLabel(request.outcome());
    } catch (IllegalArgumentException ex) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "validation", "Outcome must be emailed or skipped.", "outcome");
    }
    if (outcome != NotifyStatus.EMAILED && outcome != NotifyStatus.SKIPPED) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "validation", "Outcome must be emailed or skipped.", "outcome");
    }
    Instant now = Instant.now();
    task.setNotifyStatus(outcome);
    task.setNotifyTo(task.getAssigneeUsername());
    task.setNotifyAt(now);
    task.setNotifyBy(actor.getUsername());
    task.setUpdatedAt(now);
    task.setUpdatedByAccount(actor.getUsername());
    tasks.save(task);
    return toResponse(task, actor, LocalDate.now(AiracController.UNIT_ZONE));
  }

  @Transactional
  public void markSeenForCurrentUser() {
    AccountPrincipal actor = AuthService.currentPrincipal()
        .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "auth", "Not signed in."));
    Instant now = Instant.now();
    List<Task> mine = tasks.findByRetiredFalse().stream()
        .filter(t -> actor.getUsername().equalsIgnoreCase(t.getAssigneeUsername()))
        .filter(t -> t.getSeenAt() == null)
        .filter(t -> t.getStatus() != TaskStatus.DONE)
        .toList();
    for (Task task : mine) {
      task.setSeenAt(now);
    }
    tasks.saveAll(mine);
  }

  @Transactional
  public void delete(UUID id) {
    AccountPrincipal actor = requireEditor();
    Task task = requireActive(id);
    task.setRetired(true);
    task.setUpdatedAt(Instant.now());
    task.setUpdatedByAccount(actor.getUsername());
    tasks.save(task);
  }

  private void apply(Task task, UpsertTaskRequest request, AccountPrincipal actor, Instant now, boolean creating) {
    task.setTitle(request.title().trim());
    task.setDueDate(request.dueDate());
    task.setPriority(request.priority());
    try {
      TaskStatus status = TaskStatus.fromLabel(request.status());
      task.setStatus(status);
      if (status == TaskStatus.DONE && task.getCompletedAt() == null) {
        task.setCompletedAt(now);
      }
      if (status != TaskStatus.DONE) {
        task.setCompletedAt(null);
      }
    } catch (IllegalArgumentException ex) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "validation", "Status must be To do, In progress, or Done.", "status");
    }

    String username = blankToNull(request.assigneeUsername());
    if (username == null) {
      username = actor.getUsername();
    }
    Account assignee = accounts.findByUsernameIgnoreCase(username)
        .filter(Account::isActive)
        .orElseThrow(() -> new ApiException(HttpStatus.BAD_REQUEST, "validation", "Choose an active account as assignee.", "assigneeUsername"));

    boolean assigneeChanged = creating || !Objects.equals(
        task.getAssigneeUsername() == null ? null : task.getAssigneeUsername().toLowerCase(Locale.ROOT),
        assignee.getUsername().toLowerCase(Locale.ROOT)
    );
    task.setAssigneeUsername(assignee.getUsername());
    task.setAssigneeName(assignee.getDisplayName());
    task.setAssigneeDesignation(assignee.getDesignation());
    task.setAssigneeEmail(assignee.getEmail());

    if (assigneeChanged) {
      task.setAssignedAt(now);
      task.setAssignedBy(actor.getUsername());
      task.setAssignedByName(actor.getDisplayName());
      task.setSeenAt(assignee.getUsername().equalsIgnoreCase(actor.getUsername()) ? now : null);
      if (assignee.getUsername().equalsIgnoreCase(actor.getUsername())) {
        task.setNotifyStatus(NotifyStatus.NOT_NEEDED);
        task.setNotifyTo(null);
        task.setNotifyAt(null);
        task.setNotifyBy(null);
      } else {
        task.setNotifyStatus(NotifyStatus.PENDING);
        task.setNotifyTo(assignee.getUsername());
        task.setNotifyAt(null);
        task.setNotifyBy(null);
      }
    }
  }

  private boolean matchesFilter(Task task, String filter, AccountPrincipal me, LocalDate today) {
    return switch (filter.toLowerCase(Locale.ROOT)) {
      case "all" -> true;
      case "done" -> task.getStatus() == TaskStatus.DONE;
      case "mine" -> me != null && me.getUsername().equalsIgnoreCase(task.getAssigneeUsername());
      case "overdue" -> isOverdue(task, today);
      case "open" -> task.getStatus() != TaskStatus.DONE;
      default -> throw new ApiException(HttpStatus.BAD_REQUEST, "validation", "Unknown filter.", "filter");
    };
  }

  private Comparator<Task> taskComparator(LocalDate today) {
    return Comparator
        .comparing((Task t) -> !isOverdue(t, today))
        .thenComparing(Task::isPriority, Comparator.reverseOrder())
        .thenComparing(Task::getDueDate)
        .thenComparing(Task::getCreatedAt, Comparator.reverseOrder());
  }

  private boolean isOverdue(Task task, LocalDate today) {
    return task.getStatus() != TaskStatus.DONE && task.getDueDate().isBefore(today);
  }

  private TaskResponse toResponse(Task task, AccountPrincipal me, LocalDate today) {
    boolean overdue = isOverdue(task, today);
    Integer daysLate = overdue ? (int) ChronoUnit.DAYS.between(task.getDueDate(), today) : null;
    boolean newForYou = me != null
        && me.getUsername().equalsIgnoreCase(task.getAssigneeUsername())
        && task.getSeenAt() == null
        && task.getStatus() != TaskStatus.DONE;
    return new TaskResponse(
        task.getId(),
        task.getTitle(),
        task.getDueDate(),
        task.getStatus().getLabel(),
        task.isPriority(),
        overdue,
        daysLate,
        task.getAssigneeUsername(),
        task.getAssigneeName(),
        task.getAssigneeDesignation(),
        task.getAssigneeEmail(),
        initials(task.getAssigneeName()),
        task.getAssignedAt(),
        task.getAssignedBy(),
        task.getAssignedByName(),
        newForYou,
        task.getSeenAt(),
        task.getCompletedAt(),
        task.getNotifyStatus().getLabel(),
        task.getNotifyAt(),
        task.getNotifyBy(),
        task.isExample(),
        task.getCreatedAt(),
        task.getCreatedByAccount(),
        task.getUpdatedAt()
    );
  }

  private static String initials(String name) {
    if (name == null || name.isBlank()) {
      return "?";
    }
    String[] parts = name.trim().split("\\s+");
    StringBuilder sb = new StringBuilder();
    for (String part : parts) {
      if (!part.isEmpty()) {
        sb.append(Character.toUpperCase(part.charAt(0)));
      }
      if (sb.length() >= 2) {
        break;
      }
    }
    return sb.toString();
  }

  private Task requireActive(UUID id) {
    return tasks.findByIdAndRetiredFalse(id)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "not-found", "Task not found."));
  }

  private AccountPrincipal requireEditor() {
    AccountPrincipal principal = AuthService.currentPrincipal()
        .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "auth", "Not signed in."));
    if (principal.getRole() == AccountRole.VIEWER) {
      throw new ApiException(
          HttpStatus.FORBIDDEN,
          "forbidden",
          "Your account has the Viewer role, so you can read records but not change them."
      );
    }
    return principal;
  }

  private static String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}
