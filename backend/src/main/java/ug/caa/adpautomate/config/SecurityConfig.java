package ug.caa.adpautomate.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.session.SessionManagementFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

  private final SessionAbsoluteTimeoutFilter absoluteTimeoutFilter;

  public SecurityConfig(SessionAbsoluteTimeoutFilter absoluteTimeoutFilter) {
    this.absoluteTimeoutFilter = absoluteTimeoutFilter;
  }

  @Bean
  SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
    http
        .csrf(csrf -> csrf.disable())
        .cors(Customizer.withDefaults())
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
        .securityContext(sc -> sc.securityContextRepository(securityContextRepository()))
        .authorizeHttpRequests(auth -> auth
            .requestMatchers("/actuator/health", "/actuator/info", "/api/ping").permitAll()
            .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
            .requestMatchers(HttpMethod.GET, "/api/auth/status").permitAll()
            .requestMatchers(HttpMethod.POST, "/api/auth/setup", "/api/auth/login").permitAll()
            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
            .requestMatchers("/api/users/**").hasRole("ADMINISTRATOR")
            .anyRequest().authenticated()
        )
        .exceptionHandling(ex -> ex.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
        .httpBasic(basic -> basic.disable())
        .formLogin(form -> form.disable())
        .logout(logout -> logout.disable())
        .addFilterAfter(absoluteTimeoutFilter, SessionManagementFilter.class);

    return http.build();
  }

  @Bean
  SecurityContextRepository securityContextRepository() {
    return new HttpSessionSecurityContextRepository();
  }

  @Bean
  CorsConfigurationSource corsConfigurationSource(
      org.springframework.core.env.Environment environment
  ) {
    CorsConfiguration config = new CorsConfiguration();
    String configured = environment.getProperty("CORS_ORIGINS", "http://localhost:5173");
    List<String> origins = List.of(configured.split(","));
    // Patterns keep localhost + Render working; exact origins from env are also allowed.
    config.setAllowedOriginPatterns(List.of(
        "http://localhost:*",
        "https://*.onrender.com"
    ));
    for (String origin : origins) {
      String trimmed = origin.trim();
      if (!trimmed.isEmpty()) {
        config.addAllowedOriginPattern(trimmed);
      }
    }
    config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    config.setAllowedHeaders(List.of("*"));
    config.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", config);
    return source;
  }
}
