package ug.caa.adpautomate.auth;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

  private final PasswordHasher hasher = new PasswordHasher();

  @Test
  void hashesAndVerifiesPassword() {
    var parts = hasher.hash("Secret123");
    assertTrue(hasher.matches("Secret123", parts.salt(), parts.hash(), parts.iter()));
    assertFalse(hasher.matches("Wrong1234", parts.salt(), parts.hash(), parts.iter()));
  }
}
