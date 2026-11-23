package ug.caa.adpautomate.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ug.caa.adpautomate.auth.AuthService;

import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class SessionAbsoluteTimeoutFilter extends OncePerRequestFilter {

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {
    HttpSession session = request.getSession(false);
    if (session != null) {
      Object signedInAt = session.getAttribute(AuthService.SESSION_SIGNED_IN_AT);
      if (signedInAt instanceof String value) {
        Instant at = Instant.parse(value);
        if (at.plus(AuthService.SESSION_MAX_HOURS, ChronoUnit.HOURS).isBefore(Instant.now())) {
          SecurityContextHolder.clearContext();
          session.invalidate();
        }
      }
    }
    filterChain.doFilter(request, response);
  }
}
