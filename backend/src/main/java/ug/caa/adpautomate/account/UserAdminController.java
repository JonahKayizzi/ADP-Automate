package ug.caa.adpautomate.account;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ug.caa.adpautomate.account.dto.UserDtos.CreateUserRequest;
import ug.caa.adpautomate.account.dto.UserDtos.ResetPasswordRequest;
import ug.caa.adpautomate.account.dto.UserDtos.UpdateUserRequest;
import ug.caa.adpautomate.account.dto.UserDtos.UserListItem;
import ug.caa.adpautomate.auth.dto.AuthDtos.UserResponse;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserAdminController {

  private final UserAdminService userAdminService;

  public UserAdminController(UserAdminService userAdminService) {
    this.userAdminService = userAdminService;
  }

  @GetMapping
  public List<UserListItem> list() {
    return userAdminService.list();
  }

  @PostMapping
  public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
    return userAdminService.create(request);
  }

  @PatchMapping("/{id}")
  public UserResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
    return userAdminService.update(id, request);
  }

  @PostMapping("/{id}/reset-password")
  public UserResponse resetPassword(@PathVariable UUID id, @Valid @RequestBody ResetPasswordRequest request) {
    return userAdminService.resetPassword(id, request);
  }
}
