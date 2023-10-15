package io.sandbox.dimensions.configs;

import io.sandbox.dimensions.dimension.DimensionSave;
import net.fabricmc.fabric.api.dimension.v1.FabricDimensions;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;

public class PlayerRespawnConfig {
  public static void initRespawnListener() {
    // This tails the respawn event and can probably be used to teleport the player
    ServerPlayerEvents.AFTER_RESPAWN.register((
      ServerPlayerEntity oldPlayer,
      ServerPlayerEntity newPlayer,
      boolean alive
    ) -> {
      ServerWorld dimension = oldPlayer.getServerWorld();
      DimensionSave dimensionSave = DimensionSave.getDimensionState(dimension);
      Boolean shouldKeepInventory = dimensionSave.getRule(DimensionSave.KEEP_INVENTORY_ON_DEATH);
      Boolean respawnInDimension = dimensionSave.getRule(DimensionSave.RESPAWN_IN_DIMENSION);
      if (shouldKeepInventory) {
        newPlayer.getInventory().clone(oldPlayer.getInventory());
      }

      if (respawnInDimension) {
        // Teleport new PlayerEntity to spawn location of dimension
        BlockPos spawnPos = dimensionSave.getSpawnPos(dimension);
        FabricDimensions.teleport(
          newPlayer,
          dimension,
          new TeleportTarget(
            new Vec3d(
              spawnPos.getX() + 0.5,
              spawnPos.getY() + 1,
              spawnPos.getZ() + 0.5
            ),
            new Vec3d(0, 0, 0),
            newPlayer.getYaw(),
            newPlayer.getPitch()
          )
        );
      }
    });
  }
}
