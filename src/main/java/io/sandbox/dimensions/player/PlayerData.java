package io.sandbox.dimensions.player;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;

public class PlayerData {
  public List<PlayerPosition> previousPositions = new ArrayList<>();

  public NbtCompound getPlayerDataNbt() {
    NbtCompound playerNbt = new NbtCompound();
    NbtList previousPositionsNbt = new NbtList();
    this.previousPositions.forEach((playerPosition) -> {
      NbtCompound playerPosNbt = new NbtCompound();
      playerPosNbt.putString("dimension", playerPosition.dimension);
      playerPosNbt.putInt("posX", playerPosition.posX);
      playerPosNbt.putInt("posY", playerPosition.posY);
      playerPosNbt.putInt("posZ", playerPosition.posZ);
      previousPositionsNbt.add(playerPosNbt);
    });

    playerNbt.put("previousPositions", previousPositionsNbt);

    return playerNbt;
  }
}
