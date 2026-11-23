package ug.caa.adpautomate.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * Accepts Render-style {@code postgres://user:pass@host:5432/db} DATABASE_URL values
 * and maps them to Spring JDBC properties.
 */
public class RenderDatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

  @Override
  public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
    String raw = environment.getProperty("DATABASE_URL");
    if (raw == null || raw.isBlank()) {
      return;
    }

    Map<String, Object> props = new HashMap<>();
    boolean onRender = "true".equalsIgnoreCase(environment.getProperty("RENDER"))
        || environment.getProperty("RENDER_SERVICE_ID") != null;

    if (raw.startsWith("postgres://") || raw.startsWith("postgresql://")) {
      try {
        URI uri = new URI(raw.replaceFirst("^postgres://", "postgresql://"));
        String userInfo = uri.getUserInfo();
        if (userInfo != null && !userInfo.isBlank()) {
          String[] parts = userInfo.split(":", 2);
          props.put("spring.datasource.username", decode(parts[0]));
          if (parts.length > 1) {
            props.put("spring.datasource.password", decode(parts[1]));
          }
        }
        String path = uri.getPath() == null ? "" : uri.getPath();
        String database = path.startsWith("/") ? path.substring(1) : path;
        int port = uri.getPort() > 0 ? uri.getPort() : 5432;
        String jdbc = "jdbc:postgresql://" + uri.getHost() + ":" + port + "/" + database;
        if (onRender || raw.contains("render.com") || uri.getHost() != null && uri.getHost().contains("render.com")) {
          jdbc = jdbc + "?sslmode=require";
        }
        props.put("spring.datasource.url", jdbc);
      } catch (URISyntaxException ex) {
        throw new IllegalStateException("Invalid DATABASE_URL", ex);
      }
    } else if (raw.startsWith("jdbc:postgresql://") && onRender && !raw.contains("sslmode=")) {
      props.put("spring.datasource.url", raw + (raw.contains("?") ? "&" : "?") + "sslmode=require");
    }

    if (onRender) {
      props.put("server.servlet.session.cookie.same-site", "none");
      props.put("server.servlet.session.cookie.secure", "true");
      props.put("server.forward-headers-strategy", "framework");
    }

    if (!props.isEmpty()) {
      environment.getPropertySources().addFirst(new MapPropertySource("renderDatabaseUrl", props));
    }
  }

  private static String decode(String value) {
    return URLDecoder.decode(value, StandardCharsets.UTF_8);
  }

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 10;
  }
}
