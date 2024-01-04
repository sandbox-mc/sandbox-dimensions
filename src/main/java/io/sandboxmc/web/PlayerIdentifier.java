package io.sandboxmc.web;

import net.minecraft.server.network.ServerPlayerEntity;

public class PlayerIdentifier {
  private ServerPlayerEntity player;
  private String playerUUID;
  private String playerIP;
  private String serverAddress;
  private String serverName;
  private String jsonAuthString = null;

  public PlayerIdentifier(ServerPlayerEntity thePlayer) {
    player = thePlayer;
    playerUUID = player.getUuidAsString();
    playerIP = player.getIp();
    serverAddress = player.getServer().getServerIp() + ":" + player.getServer().getServerPort();
    serverName = player.getServer().getName();
  }

  public String getUUID() {
    return playerUUID;
  }

  public String getIdentifier() {
    return serverAddress + ":" + playerIP + ":" + playerUUID;
  }

  public String getJSON() {
    if (jsonAuthString != null) {
      return jsonAuthString;
    }

    String jsonString = "{";
    jsonString += "\"uuid\": \"" + playerUUID + "\",";
    jsonString += "\"player_ip\": \"" + playerIP + "\",";
    jsonString += "\"server_address\": \"" + serverAddress + "\",";
    jsonString += "\"server_name\": \"" + serverName + "\"";
    jsonString += "}";
    jsonAuthString = jsonString;
    return jsonAuthString;
  }
}
