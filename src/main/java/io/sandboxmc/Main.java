package io.sandboxmc;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import io.sandboxmc.configs.PlayerRespawnConfig;
import io.sandboxmc.dimension.DimensionManager;
import io.sandboxmc.eventListeners.ServerStartedListener;
import io.sandboxmc.eventListeners.ServerStartingListener;
import io.sandboxmc.eventListeners.ServerStoppedListener;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import io.sandboxmc.chunkGenerators.ChunkGeneratorManager;
import io.sandboxmc.commands.CommandInit;

public class Main implements ModInitializer {
  public static final String MOD_ID = "sandboxmc";
  private static final String CONFIG_FILE = "sandboxmc.properties";
  private static String env = null;

  public static String getEnv() {
    return env;
  }

  private static void setEnv(String theEnv) {
    if (theEnv == null) {
      env = "PRODUCTION";
      return;
    }

    switch (theEnv.toLowerCase()) {
      case "test":
        // Will use production website but should put loggers into test mode.
        env = "TEST";
        break;
      case "local":
        // Will connect to LOCAL website (127.0.0.1:3000)
        // Loggers will be treated as "TEST"
        env = "LOCAL";
        break;
      case "dev":
      case "development":
        // Used for connecting to a local version of the website (sandboxmc.dev)
        // Loggers will be treated as "TEST"
        env = "DEVELOPMENT";
        break;
      default:
        // Connects to production site (sandboxmc.io)
        env = "PRODUCTION";
        break;
    }

    Web.setWebDomainByEnv();
  }

  // First thing called in ServerStartingListener.
  public static void setConfigs(MinecraftServer server) {
    if (env != null) return;

    File configFile = server.getFile(CONFIG_FILE);
    if (!configFile.exists()) {
      // no config file, set defaults
      // if we end up with more settings that aren't strictly for development stuff
      // we may want to create a "defaults" file.
      // `environment` should never be in the defaults, however.
      setEnv("production");
      return;
    }

    try {
      BufferedReader reader = Files.newBufferedReader(configFile.toPath());
      while (reader.ready()) {
        String line = reader.readLine();
        if (line.startsWith("#") || line.length() == 0) continue;

        String[] setting = line.split("=");
        if (setting.length != 2) continue;

        switch (setting[0].toLowerCase()) {
          case "env":
          case "environment":
            setEnv(setting[1]);
            break;
          default:
            break;
        }
      }
      reader.close();
    } catch (IOException e) {
      // file accessor error of some sort
    }
  }

  @Override
  public void onInitialize() {
    // This code runs as soon as Minecraft is in a mod-load-ready state.
    // However, some things (like resources) may still be uninitialized.
    // Proceed with mild caution.

    ServerLifecycleEvents.SERVER_STARTING.register(new ServerStartingListener());
    ServerLifecycleEvents.SERVER_STARTED.register(new ServerStartedListener());
    ServerLifecycleEvents.SERVER_STOPPED.register(new ServerStoppedListener());

    // Initialize Commands
    ChunkGeneratorManager.init();
    CommandInit.init();
    DimensionManager.init();
    PlayerRespawnConfig.initRespawnListener();
  }

  public static Identifier id(String name) {
    return new Identifier(Main.MOD_ID, name);
  }
}
