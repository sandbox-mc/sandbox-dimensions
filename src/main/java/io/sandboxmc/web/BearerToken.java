package io.sandboxmc.web;

public class BearerToken {
  private String token;
  private long lastAccessed;
  private boolean expireable = true;

  public BearerToken(String theToken, long tokenLastAccessed) {
    token = theToken;
    lastAccessed = tokenLastAccessed;
  }

  public String getToken() {
    return token;
  }

  public long getLastAccessed() {
    return lastAccessed;
  }

  public void setExpireable(boolean isExpireable) {
    expireable = isExpireable;
  }

  public boolean getExpireable() {
    return expireable;
  }
}
