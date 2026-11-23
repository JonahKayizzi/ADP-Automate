package ug.caa.adpautomate.auth;

import java.util.regex.Pattern;

public final class PasswordRules {

  private static final Pattern USERNAME =
      Pattern.compile("^[a-z0-9][a-z0-9._-]{2,31}$");
  private static final Pattern HAS_LETTER = Pattern.compile(".*[A-Za-z].*");
  private static final Pattern HAS_DIGIT = Pattern.compile(".*\\d.*");

  private PasswordRules() {}

  public static boolean isValidUsername(String username) {
    return username != null && USERNAME.matcher(username).matches();
  }

  public static boolean isValidPassword(String password) {
    return password != null
        && password.length() >= 8
        && HAS_LETTER.matcher(password).matches()
        && HAS_DIGIT.matcher(password).matches();
  }
}
