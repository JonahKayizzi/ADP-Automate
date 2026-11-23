package ug.caa.adpautomate.auth;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import ug.caa.adpautomate.account.Account;
import ug.caa.adpautomate.account.AccountRole;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class AccountPrincipal implements UserDetails {

  private final UUID id;
  private final String username;
  private final String displayName;
  private final String designation;
  private final AccountRole role;
  private final boolean mustChange;
  private final boolean active;

  public AccountPrincipal(Account account) {
    this.id = account.getId();
    this.username = account.getUsername();
    this.displayName = account.getDisplayName();
    this.designation = account.getDesignation();
    this.role = account.getRole();
    this.mustChange = account.isMustChange();
    this.active = account.isActive();
  }

  public UUID getId() {
    return id;
  }

  public String getDisplayName() {
    return displayName;
  }

  public String getDesignation() {
    return designation;
  }

  public AccountRole getRole() {
    return role;
  }

  public boolean isMustChange() {
    return mustChange;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
  }

  @Override
  public String getPassword() {
    return "";
  }

  @Override
  public String getUsername() {
    return username;
  }

  @Override
  public boolean isAccountNonExpired() {
    return true;
  }

  @Override
  public boolean isAccountNonLocked() {
    return active;
  }

  @Override
  public boolean isCredentialsNonExpired() {
    return true;
  }

  @Override
  public boolean isEnabled() {
    return active;
  }
}
