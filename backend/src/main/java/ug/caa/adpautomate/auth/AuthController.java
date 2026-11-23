package ug.caa.adpautomate.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ug.caa.adpautomate.auth.dto.AuthDtos.AuthStatusResponse;
import ug.caa.adpautomate.auth.dto.AuthDtos.ChangePasswordRequest;
import ug.caa.adpautomate.auth.dto.AuthDtos.LoginRequest;
import ug.caa.adpautomate.auth.dto.AuthDtos.LoginResponse;
import ug.caa.adpautomate.auth.dto.AuthDtos.MessageResponse;
import ug.caa.adpautomate.auth.dto.AuthDtos.SetupRequest;
import ug.caa.adpautomate.auth.dto.AuthDtos.UserResponse;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @GetMapping("/status")
  public AuthStatusResponse status() {
    return authService.status();
  }

  @PostMapping("/setup")
  public UserResponse setup(@Valid @RequestBody SetupRequest request, HttpServletRequest httpRequest) {
    return authService.setup(request, httpRequest);
  }

  @PostMapping("/login")
  public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
    return authService.login(request, httpRequest);
  }

  @PostMapping("/logout")
  public MessageResponse logout(HttpServletRequest request) {
    authService.logout(request);
    return new MessageResponse("Signed out");
  }

  @GetMapping("/me")
  public UserResponse me() {
    return authService.me();
  }

  @PostMapping("/change-password")
  public UserResponse changePassword(
      @Valid @RequestBody ChangePasswordRequest request,
      HttpServletRequest httpRequest
  ) {
    boolean requireCurrent = AuthService.currentPrincipal()
        .map(p -> !p.isMustChange())
        .orElse(true);
    return authService.changePassword(request, requireCurrent, httpRequest);
  }

  @PostMapping("/unlock")
  public MessageResponse unlock(@Valid @RequestBody LoginRequest request) {
    authService.unlock(request);
    return new MessageResponse("Unlocked");
  }
}
