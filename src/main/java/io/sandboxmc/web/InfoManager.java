package io.sandboxmc.web;

import com.google.gson.internal.JavaVersion;

import io.sandboxmc.Main;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;

public class InfoManager {
  public static final String WEB_DOMAIN = "https://www.sandboxmc.dev";
  public static final String MOD_VERSION = FabricLoader.getInstance().getModContainer(Main.modId).get().toString().replace("sandboxmc ", "");

  public static String userAgent(MinecraftServer server) {
    return "SandboxMC Agent (" + MOD_VERSION + "); Minecraft (" + server.getVersion() + "); Java (" + JavaVersion.getMajorJavaVersion() + ");";
  }
}
