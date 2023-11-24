package io.sandboxmc.web;

import java.util.HashMap;

public class AuthManager {
  private static HashMap<String, String> bearerTokens = new HashMap<String, String>();

  public static String getBearerToken(String key) {
    // TODO: handle inactivity timeouts
    return bearerTokens.get(key);
  }

  public static void setBearerToken(String key, String value) {
    bearerTokens.put(key, value);
  }
}
