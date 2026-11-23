package ug.caa.adpautomate.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.util.HashMap;
import java.util.Map;

/**
 * Accepts Render-style {@code postgres://user:pass@host:5432/db} DATABASE_URL values
 * and maps them to Spring JDBC properties.
 */
public class RenderDatabaseUrlEnvironmentPostProcessor implements EnvironmentPostProcessor, Ordered {

  @Override
  public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
    Map<String, Object> props = new HashMap<>();

    boolean onRender = "true".equalsIgnoreCase(environment.getProperty("RENDER"))
        || environment.getProperty("RENDER_SERVICE_ID") != null;

    DatabaseUrlSupport.Converted converted = DatabaseUrlSupport.convert(environment.getProperty("DATABASE_URL"));
    if (converted != null) {
      props.put("spring.datasource.url", converted.jdbcUrl());
      props.put("JDBC_DATABASE_URL", converted.jdbcUrl());
      if (converted.username() != null) {
        props.put("spring.datasource.username", converted.username());
        props.put("DATABASE_USER", converted.username());
      }
      if (converted.password() != null) {
        props.put("spring.datasource.password", converted.password());
        props.put("DATABASE_PASSWORD", converted.password());
      }
    } else if (onRender) {
      String jdbc = environment.getProperty("JDBC_DATABASE_URL");
      if (jdbc != null && jdbc.startsWith("jdbc:postgresql://") && !jdbc.contains("sslmode=")) {
        props.put("spring.datasource.url", jdbc + (jdbc.contains("?") ? "&" : "?") + "sslmode=require");
        props.put("JDBC_DATABASE_URL", props.get("spring.datasource.url"));
      }
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

  @Override
  public int getOrder() {
    return Ordered.HIGHEST_PRECEDENCE + 10;
  }
}
