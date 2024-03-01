package io.sandboxmc.web;

import java.util.UUID;

import net.minecraft.server.network.ServerPlayerEntity;

public class PlayerIdentifier {
  public ServerPlayerEntity player;
  private String playerUUID;
  private String playerIP;
  private String serverAddress;
  private String serverName;

  public static UUID uuidFromTrimmed(String trimmedUUID) throws IllegalArgumentException {
    if (trimmedUUID == null) {
      throw new IllegalArgumentException();
    }

    StringBuilder builder = new StringBuilder(trimmedUUID.trim());
    // Backwards adding to avoid index adjustments
    try {
      builder.insert(20, "-");
      builder.insert(16, "-");
      builder.insert(12, "-");
      builder.insert(8, "-");
    } catch (StringIndexOutOfBoundsException e) {
      throw new IllegalArgumentException();
    }
 
    return UUID.fromString(builder.toString());
  }

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
    StringBuilder jsonString = new StringBuilder("{");
    jsonString.append("\"uuid\": \"" + playerUUID + "\",");
    jsonString.append("\"player_ip\": \"" + playerIP + "\",");
    jsonString.append("\"server_address\": \"" + serverAddress + "\",");
    jsonString.append("\"server_name\": \"" + serverName + "\"");
    jsonString.append("}");
    return jsonString.toString();
  }
}
