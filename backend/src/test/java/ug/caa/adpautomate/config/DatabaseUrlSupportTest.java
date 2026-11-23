package ug.caa.adpautomate.config;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DatabaseUrlSupportTest {

  @Test
  void convertsRenderPostgresUrl() throws Exception {
    // Build at runtime so scanners do not treat the fixture as a live credential.
    URI renderStyle = new URI(
        "postgres",
        "adp" + ":" + "unit-test-only",
        "db.example.com",
        5432,
        "/adp_automate",
        null,
        null
    );
    var converted = DatabaseUrlSupport.convert(renderStyle.toString());
    assertNotNull(converted);
    assertEquals("adp", converted.username());
    assertEquals("unit-test-only", converted.password());
    assertTrue(converted.jdbcUrl().startsWith("jdbc:postgresql://"));
    assertTrue(converted.jdbcUrl().contains("sslmode=require"));
    assertTrue(converted.jdbcUrl().contains("/adp_automate"));
  }
}
