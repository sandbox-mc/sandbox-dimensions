package io.sandboxmc.web;

import com.mojang.brigadier.context.CommandContext;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;

public class ServerAuth extends Common implements Runnable {
  // Class level methods for interacting with the server's auth.
  private static String uuid = null;
  private static String authToken = null;

  public static void authOnBoot(MinecraftServer server) {
    Runnable serverAuthThread = new ServerAuth(server);
    new Thread(serverAuthThread).start();
  }

  public static String getUUID() {
    return uuid;
  }

  public static void setUUID(String newUUID) {
    uuid = newUUID;
  }

  public static String getAuthToken() {
    return authToken;
  }

  public static void setAuthToken(String newAuthToken) {
    authToken = newAuthToken;
  }

  // Instance methods for authing server. This all runs as a thread.
  private MinecraftServer server;

  public ServerAuth(CommandContext<ServerCommandSource> theContext) {
    super(theContext);

    server = source.getServer();
  }

  // Intended for use with 
  public ServerAuth(MinecraftServer theServer) {
    server = theServer;
  }

  public void run() {
    // TODO: attempt to read uuid and auth token from file
  }
}
