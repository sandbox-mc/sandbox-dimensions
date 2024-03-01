package io.sandboxmc.web;

import net.minecraft.server.MinecraftServer;

public class ServerIdentifier {
  public MinecraftServer server;

  public ServerIdentifier(MinecraftServer theServer) {
    server = theServer;
  }

  public String getJSON(String authToken) {
    StringBuilder sb = new StringBuilder("{\"server\": {");
    sb.append("\"address\": \"" + server.getServerIp() + ":" + server.getServerPort() + "\",");
    sb.append("\"name\": \"" + server.getName().replaceAll("\"", "__QUOTE__") + "\",");
    sb.append("\"motd\": \"" + server.getServerMotd().replaceAll("\"", "__QUOTE__") + "\"");
    if (authToken != null) {
      sb.append(", \"auth_token\": \"" + authToken + "\"");
    }
    sb.append("}}");
    return sb.toString();
  }
}
