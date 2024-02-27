package io.sandboxmc.web;

import net.minecraft.server.MinecraftServer;

public class ServerIdentifier {
  private MinecraftServer server;
  private String json = null;

  public ServerIdentifier(MinecraftServer theServer) {
    server = theServer;
  }

  public String getJSON(String authToken) {
    if (json != null) {
      return json;
    }

    StringBuilder sb = new StringBuilder("{\"server\": {");
    sb.append("\"address\": \"" + server.getServerIp() + ":" + server.getServerPort() + "\",");
    sb.append("\"name\": \"" + server.getName() + "\",");
    sb.append("\"motd\": \"" + server.getServerMotd() + "\""); // TODO: can this have quotes...?
    sb.append("}");
    if (authToken != null) {
      sb.append(", \"auth_token\": \"" + authToken + "\"");
    }
    sb.append("}");
    json = sb.toString();
    return json;
  }
}
