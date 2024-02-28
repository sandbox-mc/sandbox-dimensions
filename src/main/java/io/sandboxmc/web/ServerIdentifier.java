package io.sandboxmc.web;

import net.minecraft.server.MinecraftServer;

public class ServerIdentifier {
  private MinecraftServer server;

  public ServerIdentifier(MinecraftServer theServer) {
    server = theServer;
  }

  public String getJSON(String authToken) {
    StringBuilder sb = new StringBuilder("{\"server\": {");
    sb.append("\"address\": \"" + server.getServerIp() + ":" + server.getServerPort() + "\",");
    sb.append("\"name\": \"" + server.getName() + "\","); // TODO: can this have quotes...?
    sb.append("\"motd\": \"" + server.getServerMotd() + "\""); // TODO: can this have quotes...?
    if (authToken != null) {
      sb.append(", \"auth_token\": \"" + authToken + "\"");
    }
    sb.append("}}");
    return sb.toString();
  }
}
