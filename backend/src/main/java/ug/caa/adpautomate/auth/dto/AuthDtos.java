package ug.caa.adpautomate.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import ug.caa.adpautomate.account.AccountRole;

import java.util.UUID;

public final class AuthDtos {

  private AuthDtos() {}

  public record AuthStatusResponse(
      boolean setupRequired,
      boolean authenticated,
      UserResponse user
  ) {}

  public record UserResponse(
      UUID id,
      String username,
      String displayName,
      String designation,
      String email,
      AccountRole role,
      boolean mustChange
  ) {}

  public record SetupRequest(
      @NotBlank @Size(max = 200) String fullName,
      @Size(max = 120) String designation,
      @NotBlank String username,
      @NotBlank String password,
      @NotBlank String confirmPassword
  ) {}

  public record LoginRequest(
      @NotBlank String username,
      @NotBlank String password
  ) {}

  public record LoginResponse(
      UserResponse user,
      Integer attemptsLeft
  ) {}

  public record ChangePasswordRequest(
      String currentPassword,
      @NotBlank String newPassword,
      @NotBlank String confirmPassword
  ) {}

  public record MessageResponse(String message) {}
}
