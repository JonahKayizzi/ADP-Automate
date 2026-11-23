package ug.caa.adpautomate.account.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import ug.caa.adpautomate.account.AccountRole;

import java.time.Instant;
import java.util.UUID;

public final class UserDtos {

  private UserDtos() {}

  public record UserListItem(
      UUID id,
      String username,
      String displayName,
      String designation,
      String email,
      AccountRole role,
      boolean active,
      boolean mustChange,
      Instant lockedUntil,
      Instant lastLoginAt,
      boolean you
  ) {}

  public record CreateUserRequest(
      @NotBlank String fullName,
      String designation,
      String email,
      @NotBlank String username,
      @NotNull AccountRole role,
      @NotBlank String temporaryPassword,
      @NotBlank String confirmPassword,
      boolean askForNewPassword
  ) {}

  public record UpdateUserRequest(
      @NotBlank String fullName,
      String designation,
      String email,
      AccountRole role,
      Boolean active
  ) {}

  public record ResetPasswordRequest(
      @NotBlank String temporaryPassword,
      @NotBlank String confirmPassword
  ) {}
}
