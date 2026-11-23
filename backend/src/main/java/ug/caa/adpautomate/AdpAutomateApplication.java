package ug.caa.adpautomate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import ug.caa.adpautomate.config.DatabaseUrlSupport;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class AdpAutomateApplication {

  public static void main(String[] args) {
    // Render injects postgres:// DATABASE_URL; Spring JDBC requires jdbc:postgresql://
    applyDatabaseUrlFromEnvironment();
    applyRenderSessionCookieSettings();
    SpringApplication.run(AdpAutomateApplication.class, args);
  }

  static void applyDatabaseUrlFromEnvironment() {
    DatabaseUrlSupport.Converted converted = DatabaseUrlSupport.convert(System.getenv("DATABASE_URL"));
    if (converted == null) {
      return;
    }
    System.setProperty("spring.datasource.url", converted.jdbcUrl());
    System.setProperty("JDBC_DATABASE_URL", converted.jdbcUrl());
    if (converted.username() != null) {
      System.setProperty("spring.datasource.username", converted.username());
      System.setProperty("DATABASE_USER", converted.username());
    }
    if (converted.password() != null) {
      System.setProperty("spring.datasource.password", converted.password());
      System.setProperty("DATABASE_PASSWORD", converted.password());
    }
  }

  /**
   * Frontend and API are on different Render hosts, so the session cookie must be
   * {@code SameSite=None; Secure} or the browser will not send it (401 on /api/*).
   */
  static void applyRenderSessionCookieSettings() {
    boolean onRender = "true".equalsIgnoreCase(System.getenv("RENDER"))
        || System.getenv("RENDER_SERVICE_ID") != null;
    if (!onRender) {
      return;
    }
    System.setProperty("SESSION_COOKIE_SAME_SITE", "none");
    System.setProperty("SESSION_COOKIE_SECURE", "true");
    System.setProperty("server.servlet.session.cookie.same-site", "none");
    System.setProperty("server.servlet.session.cookie.secure", "true");
  }
}
