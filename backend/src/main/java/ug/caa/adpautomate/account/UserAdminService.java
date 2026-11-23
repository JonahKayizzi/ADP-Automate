package ug.caa.adpautomate.account;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ug.caa.adpautomate.account.dto.UserDtos.CreateUserRequest;
import ug.caa.adpautomate.account.dto.UserDtos.ResetPasswordRequest;
import ug.caa.adpautomate.account.dto.UserDtos.UpdateUserRequest;
import ug.caa.adpautomate.account.dto.UserDtos.UserListItem;
import ug.caa.adpautomate.auth.AccountPrincipal;
import ug.caa.adpautomate.auth.AuthService;
import ug.caa.adpautomate.auth.PasswordHasher;
import ug.caa.adpautomate.auth.PasswordRules;
import ug.caa.adpautomate.auth.dto.AuthDtos.UserResponse;
import ug.caa.adpautomate.web.ApiException;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class UserAdminService {

  private final AccountRepository accounts;
  private final PasswordHasher passwordHasher;

  public UserAdminService(AccountRepository accounts, PasswordHasher passwordHasher) {
    this.accounts = accounts;
    this.passwordHasher = passwordHasher;
  }

  @Transactional(readOnly = true)
  public List<UserListItem> list() {
    AccountPrincipal me = requireAdmin();
    return accounts.findAllByOrderByActiveDescDisplayNameAsc().stream()
        .map(a -> toListItem(a, me.getUsername()))
        .toList();
  }

  @Transactional
  public UserResponse create(CreateUserRequest request) {
    AccountPrincipal me = requireAdmin();
    String username = request.username().trim().toLowerCase(Locale.ROOT);
    if (!PasswordRules.isValidUsername(username)) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          "validation",
          "Usernames are 3 to 32 characters: lower-case letters, digits, dot, dash or underscore.",
          "username"
      );
    }
    if (accounts.existsByUsernameIgnoreCase(username)) {
      throw new ApiException(HttpStatus.CONFLICT, "validation", "That username is already taken.", "username");
    }
    if (!PasswordRules.isValidPassword(request.temporaryPassword())) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          "validation",
          "At least 8 characters, with at least one letter and one number.",
          "temporaryPassword"
      );
    }
    if (!request.temporaryPassword().equals(request.confirmPassword())) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "validation", "Passwords do not match.", "confirmPassword");
    }

    Instant now = Instant.now();
    PasswordHasher.PasswordParts parts = passwordHasher.hash(request.temporaryPassword());
    Account account = new Account();
    account.setId(UUID.randomUUID());
    account.setUsername(username);
    account.setDisplayName(request.fullName().trim());
    account.setDesignation(blankToNull(request.designation()));
    account.setEmail(blankToNull(request.email()));
    account.setRole(request.role() == null ? AccountRole.EDITOR : request.role());
    account.setActive(true);
    account.setMustChange(request.askForNewPassword());
    account.setSalt(parts.salt());
    account.setHash(parts.hash());
    account.setIter(parts.iter());
    account.setCreatedAt(now);
    account.setUpdatedAt(now);
    account.setCreatedBy(me.getUsername());
    account.setUpdatedBy(me.getUsername());
    accounts.save(account);
    return AuthService.toUser(account);
  }

  @Transactional
  public UserResponse update(UUID id, UpdateUserRequest request) {
    AccountPrincipal me = requireAdmin();
    Account account = accounts.findById(id)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "not-found", "User not found."));

    if (me.getId().equals(account.getId())) {
      if (request.role() != null && request.role() != account.getRole()) {
        throw new ApiException(HttpStatus.BAD_REQUEST, "validation", "You cannot change your own role.");
      }
      if (request.active() != null && !request.active()) {
        throw new ApiException(HttpStatus.BAD_REQUEST, "validation", "You cannot deactivate yourself.");
      }
    }

    if (request.role() != null
        && account.getRole() == AccountRole.ADMINISTRATOR
        && request.role() != AccountRole.ADMINISTRATOR
        && accounts.countByActiveTrueAndRole(AccountRole.ADMINISTRATOR) <= 1
        && account.isActive()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "validation", "At least one active Administrator must remain.");
    }

    if (Boolean.FALSE.equals(request.active())
        && account.getRole() == AccountRole.ADMINISTRATOR
        && accounts.countByActiveTrueAndRole(AccountRole.ADMINISTRATOR) <= 1) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "validation", "At least one active Administrator must remain.");
    }

    account.setDisplayName(request.fullName().trim());
    account.setDesignation(blankToNull(request.designation()));
    account.setEmail(blankToNull(request.email()));
    if (request.role() != null) {
      account.setRole(request.role());
    }
    if (request.active() != null) {
      account.setActive(request.active());
    }
    account.setUpdatedAt(Instant.now());
    account.setUpdatedBy(me.getUsername());
    accounts.save(account);
    return AuthService.toUser(account);
  }

  @Transactional
  public UserResponse resetPassword(UUID id, ResetPasswordRequest request) {
    AccountPrincipal me = requireAdmin();
    Account account = accounts.findById(id)
        .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "not-found", "User not found."));
    if (!PasswordRules.isValidPassword(request.temporaryPassword())) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          "validation",
          "At least 8 characters, with at least one letter and one number.",
          "temporaryPassword"
      );
    }
    if (!request.temporaryPassword().equals(request.confirmPassword())) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "validation", "Passwords do not match.", "confirmPassword");
    }

    PasswordHasher.PasswordParts parts = passwordHasher.hash(request.temporaryPassword());
    account.setSalt(parts.salt());
    account.setHash(parts.hash());
    account.setIter(parts.iter());
    account.setMustChange(true);
    account.setFailed(0);
    account.setLockedUntil(null);
    account.setUpdatedAt(Instant.now());
    account.setUpdatedBy(me.getUsername());
    accounts.save(account);
    return AuthService.toUser(account);
  }

  private AccountPrincipal requireAdmin() {
    AccountPrincipal principal = AuthService.currentPrincipal()
        .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "auth", "Not signed in."));
    if (principal.getRole() != AccountRole.ADMINISTRATOR) {
      throw new ApiException(HttpStatus.FORBIDDEN, "forbidden", "Only Administrators can manage users.");
    }
    return principal;
  }

  private UserListItem toListItem(Account account, String currentUsername) {
    return new UserListItem(
        account.getId(),
        account.getUsername(),
        account.getDisplayName(),
        account.getDesignation(),
        account.getEmail(),
        account.getRole(),
        account.isActive(),
        account.isMustChange(),
        account.getLockedUntil(),
        account.getLastLoginAt(),
        account.getUsername().equalsIgnoreCase(currentUsername)
    );
  }

  private static String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}
