package ug.caa.adpautomate.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ug.caa.adpautomate.account.Account;
import ug.caa.adpautomate.account.AccountRepository;
import ug.caa.adpautomate.account.AccountRole;
import ug.caa.adpautomate.auth.dto.AuthDtos.AuthStatusResponse;
import ug.caa.adpautomate.auth.dto.AuthDtos.ChangePasswordRequest;
import ug.caa.adpautomate.auth.dto.AuthDtos.LoginRequest;
import ug.caa.adpautomate.auth.dto.AuthDtos.LoginResponse;
import ug.caa.adpautomate.auth.dto.AuthDtos.SetupRequest;
import ug.caa.adpautomate.auth.dto.AuthDtos.UserResponse;
import ug.caa.adpautomate.web.ApiException;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

  public static final int MAX_FAILED = 5;
  public static final int LOCKOUT_MINUTES = 15;
  public static final int SESSION_MAX_HOURS = 8;
  public static final String SESSION_SIGNED_IN_AT = "SIGNED_IN_AT";

  private final AccountRepository accounts;
  private final PasswordHasher passwordHasher;

  public AuthService(AccountRepository accounts, PasswordHasher passwordHasher) {
    this.accounts = accounts;
    this.passwordHasher = passwordHasher;
  }

  @Transactional(readOnly = true)
  public AuthStatusResponse status() {
    boolean setupRequired = accounts.count() == 0;
    Optional<AccountPrincipal> principal = currentPrincipal();
    if (principal.isEmpty()) {
      return new AuthStatusResponse(setupRequired, false, null);
    }
    Account account = accounts.findById(principal.get().getId())
        .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "auth", "Not signed in."));
    return new AuthStatusResponse(false, true, toUser(account));
  }

  @Transactional
  public UserResponse setup(SetupRequest request, HttpServletRequest httpRequest) {
    if (accounts.count() > 0) {
      throw new ApiException(HttpStatus.CONFLICT, "setup", "An administrator account already exists.");
    }
    validateNewCredentials(request.username(), request.password(), request.confirmPassword());

    Instant now = Instant.now();
    PasswordHasher.PasswordParts parts = passwordHasher.hash(request.password());
    Account account = new Account();
    account.setId(UUID.randomUUID());
    account.setUsername(request.username().trim().toLowerCase(Locale.ROOT));
    account.setDisplayName(request.fullName().trim());
    account.setDesignation(blankToNull(request.designation()));
    account.setRole(AccountRole.ADMINISTRATOR);
    account.setActive(true);
    account.setMustChange(false);
    account.setSalt(parts.salt());
    account.setHash(parts.hash());
    account.setIter(parts.iter());
    account.setCreatedAt(now);
    account.setUpdatedAt(now);
    account.setCreatedBy(account.getUsername());
    account.setUpdatedBy(account.getUsername());
    account.setPasswordChangedAt(now);
    account.setLastLoginAt(now);
    accounts.save(account);

    establishSession(account, httpRequest);
    return toUser(account);
  }

  @Transactional
  public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
    String username = request.username() == null ? "" : request.username().trim().toLowerCase(Locale.ROOT);
    Account account = accounts.findByUsernameIgnoreCase(username).orElse(null);

    if (account == null || !account.isActive()) {
      throw invalidCredentials(null);
    }

    Instant now = Instant.now();
    if (account.getLockedUntil() != null && account.getLockedUntil().isAfter(now)) {
      throw new ApiException(
          HttpStatus.LOCKED,
          "locked",
          "Account locked until " + account.getLockedUntil() + ". Try again later or ask an administrator to reset the password."
      );
    }

    if (!passwordHasher.matches(request.password(), account.getSalt(), account.getHash(), account.getIter())) {
      account.setFailed(account.getFailed() + 1);
      if (account.getFailed() >= MAX_FAILED) {
        account.setLockedUntil(now.plus(LOCKOUT_MINUTES, ChronoUnit.MINUTES));
        account.setFailed(0);
      }
      account.setUpdatedAt(now);
      accounts.save(account);
      int attemptsLeft = account.getLockedUntil() != null && account.getLockedUntil().isAfter(now)
          ? 0
          : MAX_FAILED - account.getFailed();
      throw invalidCredentials(attemptsLeft);
    }

    if (account.getLockedUntil() != null) {
      account.setLockedUntil(null);
    }
    account.setFailed(0);
    account.setLastLoginAt(now);
    account.setUpdatedAt(now);
    accounts.save(account);

    establishSession(account, httpRequest);
    return new LoginResponse(toUser(account), null);
  }

  @Transactional
  public UserResponse changePassword(ChangePasswordRequest request, boolean requireCurrent, HttpServletRequest httpRequest) {
    AccountPrincipal principal = currentPrincipal()
        .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "auth", "Not signed in."));
    Account account = accounts.findById(principal.getId())
        .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "auth", "Not signed in."));

    if (requireCurrent) {
      if (request.currentPassword() == null || request.currentPassword().isBlank()) {
        throw new ApiException(HttpStatus.BAD_REQUEST, "validation", "Current password is required.", "currentPassword");
      }
      if (!passwordHasher.matches(request.currentPassword(), account.getSalt(), account.getHash(), account.getIter())) {
        throw new ApiException(HttpStatus.BAD_REQUEST, "auth", "Current password is incorrect.", "currentPassword");
      }
    } else if (!account.isMustChange()) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "auth", "Password change is not required.");
    }

    if (!PasswordRules.isValidPassword(request.newPassword())) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          "validation",
          "At least 8 characters, with at least one letter and one number.",
          "newPassword"
      );
    }
    if (!request.newPassword().equals(request.confirmPassword())) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "validation", "Passwords do not match.", "confirmPassword");
    }
    if (passwordHasher.matches(request.newPassword(), account.getSalt(), account.getHash(), account.getIter())) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          "validation",
          "Choose a new password that is different from the temporary one.",
          "newPassword"
      );
    }

    Instant now = Instant.now();
    PasswordHasher.PasswordParts parts = passwordHasher.hash(request.newPassword());
    account.setSalt(parts.salt());
    account.setHash(parts.hash());
    account.setIter(parts.iter());
    account.setMustChange(false);
    account.setPasswordChangedAt(now);
    account.setUpdatedAt(now);
    account.setUpdatedBy(account.getUsername());
    accounts.save(account);

    establishSession(account, httpRequest);
    return toUser(account);
  }

  @Transactional(readOnly = true)
  public void unlock(LoginRequest request) {
    AccountPrincipal principal = currentPrincipal()
        .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "auth", "Not signed in."));
    Account account = accounts.findById(principal.getId())
        .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "auth", "Not signed in."));

    if (!account.getUsername().equalsIgnoreCase(request.username() == null ? principal.getUsername() : request.username())
        && request.username() != null
        && !request.username().isBlank()
        && !principal.getUsername().equalsIgnoreCase(request.username())) {
      // Unlock only for the signed-in account; username field is optional on lock screen.
    }

    Instant now = Instant.now();
    if (account.getLockedUntil() != null && account.getLockedUntil().isAfter(now)) {
      throw new ApiException(HttpStatus.LOCKED, "locked", "Account is locked. Ask an administrator to reset the password.");
    }
    if (!passwordHasher.matches(request.password(), account.getSalt(), account.getHash(), account.getIter())) {
      throw new ApiException(HttpStatus.UNAUTHORIZED, "auth", "Password is incorrect.");
    }
  }

  public void logout(HttpServletRequest request) {
    SecurityContextHolder.clearContext();
    HttpSession session = request.getSession(false);
    if (session != null) {
      session.invalidate();
    }
  }

  public UserResponse me() {
    AccountPrincipal principal = currentPrincipal()
        .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "auth", "Not signed in."));
    Account account = accounts.findById(principal.getId())
        .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "auth", "Not signed in."));
    return toUser(account);
  }

  public static Optional<AccountPrincipal> currentPrincipal() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof AccountPrincipal principal)) {
      return Optional.empty();
    }
    return Optional.of(principal);
  }

  private void establishSession(Account account, HttpServletRequest request) {
    AccountPrincipal principal = new AccountPrincipal(account);
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    context.setAuthentication(authentication);
    SecurityContextHolder.setContext(context);

    HttpSession session = request.getSession(true);
    session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
    session.setAttribute(SESSION_SIGNED_IN_AT, Instant.now().toString());
    session.setMaxInactiveInterval((int) ChronoUnit.HOURS.getDuration().multipliedBy(SESSION_MAX_HOURS).toSeconds());
  }

  private ApiException invalidCredentials(Integer attemptsLeft) {
    String detail = attemptsLeft == null
        ? "The username or password is incorrect."
        : "The username or password is incorrect. " + attemptsLeft
            + " attempt" + (attemptsLeft == 1 ? "" : "s") + " left before the account locks for "
            + LOCKOUT_MINUTES + " minutes.";
    ApiException ex = new ApiException(HttpStatus.UNAUTHORIZED, "auth", detail);
    return ex;
  }

  private void validateNewCredentials(String username, String password, String confirm) {
    String normalized = username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    if (!PasswordRules.isValidUsername(normalized)) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          "validation",
          "Usernames are 3 to 32 characters: lower-case letters, digits, dot, dash or underscore.",
          "username"
      );
    }
    if (!PasswordRules.isValidPassword(password)) {
      throw new ApiException(
          HttpStatus.BAD_REQUEST,
          "validation",
          "At least 8 characters, with at least one letter and one number.",
          "password"
      );
    }
    if (!password.equals(confirm)) {
      throw new ApiException(HttpStatus.BAD_REQUEST, "validation", "Passwords do not match.", "confirmPassword");
    }
  }

  public static UserResponse toUser(Account account) {
    return new UserResponse(
        account.getId(),
        account.getUsername(),
        account.getDisplayName(),
        account.getDesignation(),
        account.getEmail(),
        account.getRole(),
        account.isMustChange()
    );
  }

  private static String blankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}
