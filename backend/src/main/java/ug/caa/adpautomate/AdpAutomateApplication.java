package ug.caa.adpautomate;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class AdpAutomateApplication {

  public static void main(String[] args) {
    SpringApplication.run(AdpAutomateApplication.class, args);
  }
}
