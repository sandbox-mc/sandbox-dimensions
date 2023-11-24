package io.sandboxmc.player;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;

public class PlayerData {
  public List<PlayerPosition> previousPositions = new ArrayList<>();
  public PlayerInventory inventoryCache = null;

  public PlayerData readFromNbt(NbtCompound playerDataNbt) {
    // Get previousPosition Data from NBT
    NbtList previousPositionsNbt = playerDataNbt.getList("previousPositions", 0);
    for (int i = 0; i < previousPositionsNbt.size(); i++) {
      PlayerPosition playerPosition = new PlayerPosition();
      NbtCompound playerPosNbt = previousPositionsNbt.getCompound(i);
      playerPosition.dimension = playerPosNbt.getString("dimension");
      playerPosition.posX = playerPosNbt.getInt("posX");
      playerPosition.posY = playerPosNbt.getInt("posY");
      playerPosition.posZ = playerPosNbt.getInt("posZ");
      this.previousPositions.add(playerPosition);
    }

    if (playerDataNbt.contains("inventoryCache")) {
      NbtList inventorySlots = playerDataNbt.getList("inventoryCache", 0);
      PlayerInventory playerInventory = new PlayerInventory(null);
      playerInventory.readNbt(inventorySlots);
      this.inventoryCache = playerInventory;
    }

    return this;
  }

  public NbtCompound writePlayerDataNbt() {
    // PlayerData object as NBT
    NbtCompound playerNbt = new NbtCompound();

    // PreviousPositions list as NBT
    NbtList previousPositionsNbt = new NbtList();
    this.previousPositions.forEach((playerPosition) -> {
      NbtCompound playerPosNbt = new NbtCompound();
      playerPosNbt.putString("dimension", playerPosition.dimension);
      playerPosNbt.putInt("posX", playerPosition.posX);
      playerPosNbt.putInt("posY", playerPosition.posY);
      playerPosNbt.putInt("posZ", playerPosition.posZ);
      previousPositionsNbt.add(playerPosNbt);
    });

    // Add PreviousPositions list to PlayerData object NBT
    playerNbt.put("previousPositions", previousPositionsNbt);

    // InventoryCache if exists
    if (inventoryCache != null) {
      NbtList inventoryList = new NbtList();
      inventoryList = inventoryCache.writeNbt(inventoryList);
      playerNbt.put("inventoryCache", inventoryList);
    }

    return playerNbt;
  }
}
