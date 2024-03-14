package io.sandboxmc;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import net.minecraft.server.MinecraftServer;

public class Config {
  private static final String CONFIG_FILE = "sandboxmc.properties";

  // First thing called in ServerStartingListener.
  public static void setConfigs(MinecraftServer server) {
    Plunger.debug("Loading config file.");
    File configFile = server.getFile(CONFIG_FILE);
    if (!configFile.exists()) {
      // no config file, set any defaults
      Plunger.info("No config file, loading defaults.");
      return;
    }

    try {
      BufferedReader reader = Files.newBufferedReader(configFile.toPath());
      while (reader.ready()) {
        String line = reader.readLine();
        if (line.startsWith("#") || line.length() == 0) continue;

        String[] setting = line.split("=");
        switch (setting.length) {
          case 2:
            switch (setting[0].toLowerCase()) {
              case "loglevel":
              case "log_level":
                setLogLevel(setting[1].toLowerCase());
                break;
              case "webdev":
              case "web_dev":
                setWebDev(setting[1].toLowerCase());
              default:
                break;
            }
            break;
            // may want settings that take different numbers down the road?
          default:
            break;
        }
      }
      reader.close();
      Plunger.debug("Config file loaded.");
    } catch (IOException e) {
      Plunger.error("Failed to load config file!", e);
    }
  }

  private static void setLogLevel(String setting) {
    switch (setting) {
      case "debug":
        Plunger.setDebug();
        break;
    }
  }

  private static void setWebDev(String setting) {
    switch (setting) {
      case "true":
        Web.setDevMode();
        break;
    }
  }
}
