package io.sandboxmc.web;

import com.google.gson.internal.JavaVersion;

import net.fabricmc.loader.impl.launch.FabricLauncherBase;
import net.minecraft.server.MinecraftServer;

public class InfoManager {
  public static String WEB_DOMAIN = "https://www.sandboxmc.dev";

  public static String userAgent(MinecraftServer server) {
    // TODO: determine how we're gonna do our mod version stuff...
    System.out.println("launcher info? " + FabricLauncherBase.getProperties().toString());
    String ua = "SandboxMC Agent (MODVERSION); Minecraft (" + server.getVersion() + "); Java (" + JavaVersion.getMajorJavaVersion() + ");";
    System.out.println("Here is the UA: " + ua);
    return ua;
  }
}
