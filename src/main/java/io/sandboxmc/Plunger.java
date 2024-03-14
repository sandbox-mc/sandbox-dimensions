package io.sandboxmc;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Plunger {
  private static final Logger LOGGER = LogManager.getLogger(SandboxMC.class);
  private static final Logger DEBUG_LOGGER = LogManager.getLogger("SandboxMC Plunger");

  public static enum LogLevel { DEBUG, INFO, ERROR };
  private static LogLevel level = LogLevel.INFO;

  public static void setError() {
    level = LogLevel.ERROR;
  }
  
  public static void setInfo() {
    level = LogLevel.INFO;
  }

  public static void setDebug() {
    level = LogLevel.DEBUG;

    debug("Log level set to DEBUG");
  }

  public static void error(String message) {
    LOGGER.error(message);
  }

  public static void error(String message, Throwable errThrowable) {
    LOGGER.error(message, errThrowable);
  }

  public static void info(String message) {
    LOGGER.info(message);
  }

  public static void debug(String message) {
    if (level != LogLevel.DEBUG) return;

    // Might be a way to make these a bit more colorful or something other than just "WARN"?
    DEBUG_LOGGER.warn(message);
  }
}
