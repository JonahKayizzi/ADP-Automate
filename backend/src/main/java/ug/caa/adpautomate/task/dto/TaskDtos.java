package ug.caa.adpautomate.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public final class TaskDtos {

  private TaskDtos() {
  }

  public record TaskResponse(
      UUID id,
      String title,
      LocalDate dueDate,
      String status,
      boolean priority,
      boolean overdue,
      Integer daysLate,
      String assigneeUsername,
      String assigneeName,
      String assigneeDesignation,
      String assigneeEmail,
      String assigneeInitials,
      Instant assignedAt,
      String assignedBy,
      String assignedByName,
      boolean newForYou,
      Instant seenAt,
      Instant completedAt,
      String notifyStatus,
      Instant notifyAt,
      String notifyBy,
      boolean example,
      Instant createdAt,
      String createdByAccount,
      Instant updatedAt
  ) {
  }

  public record UpsertTaskRequest(
      @NotBlank String title,
      @NotNull LocalDate dueDate,
      @NotBlank String status,
      boolean priority,
      String assigneeUsername
  ) {
  }

  public record NotifyRequest(@NotBlank String outcome) {
  }

  public record AssigneeOption(
      String username,
      String displayName,
      String designation,
      String email
  ) {
  }
}
