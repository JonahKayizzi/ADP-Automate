package ug.caa.adpautomate.auth;

import org.springframework.stereotype.Component;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class PasswordHasher {

  public static final int ITERATIONS = 210_000;
  public static final int SALT_BYTES = 16;
  public static final int KEY_BITS = 256;

  private final SecureRandom secureRandom = new SecureRandom();

  public record PasswordParts(String salt, String hash, int iter) {}

  public PasswordParts hash(String password) {
    byte[] salt = new byte[SALT_BYTES];
    secureRandom.nextBytes(salt);
    String saltB64 = Base64.getEncoder().encodeToString(salt);
    String hashB64 = hashWithSalt(password, salt, ITERATIONS);
    return new PasswordParts(saltB64, hashB64, ITERATIONS);
  }

  public boolean matches(String password, String saltB64, String expectedHashB64, int iterations) {
    byte[] salt = Base64.getDecoder().decode(saltB64);
    String actual = hashWithSalt(password, salt, iterations);
    return MessageDigest.isEqual(
        Base64.getDecoder().decode(actual),
        Base64.getDecoder().decode(expectedHashB64)
    );
  }

  private String hashWithSalt(String password, byte[] salt, int iterations) {
    try {
      PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, KEY_BITS);
      SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
      byte[] hash = factory.generateSecret(spec).getEncoded();
      return Base64.getEncoder().encodeToString(hash);
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("Password hashing failed", e);
    }
  }
}
