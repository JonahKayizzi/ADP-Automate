package ug.caa.adpautomate.config;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public final class DatabaseUrlSupport {

  private DatabaseUrlSupport() {}

  /**
   * Convert Render/Heroku {@code postgres://} URLs into Spring JDBC settings.
   * Returns null when no conversion is needed.
   */
  public static Converted convert(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    if (!(raw.startsWith("postgres://") || raw.startsWith("postgresql://"))) {
      if (raw.startsWith("jdbc:")) {
        return new Converted(raw, null, null);
      }
      return null;
    }

    try {
      URI uri = new URI(raw.replaceFirst("^postgres://", "postgresql://"));
      String username = null;
      String password = null;
      String userInfo = uri.getUserInfo();
      if (userInfo != null && !userInfo.isBlank()) {
        String[] parts = userInfo.split(":", 2);
        username = decode(parts[0]);
        if (parts.length > 1) {
          password = decode(parts[1]);
        }
      }
      String path = uri.getPath() == null ? "" : uri.getPath();
      String database = path.startsWith("/") ? path.substring(1) : path;
      int port = uri.getPort() > 0 ? uri.getPort() : 5432;
      String jdbc = "jdbc:postgresql://" + uri.getHost() + ":" + port + "/" + database + "?sslmode=require";
      return new Converted(jdbc, username, password);
    } catch (Exception ex) {
      throw new IllegalStateException("Invalid DATABASE_URL: " + raw, ex);
    }
  }

  private static String decode(String value) {
    return URLDecoder.decode(value, StandardCharsets.UTF_8);
  }

  public record Converted(String jdbcUrl, String username, String password) {}
}
