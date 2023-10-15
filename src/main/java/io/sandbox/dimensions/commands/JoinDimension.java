package io.sandbox.dimensions.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.sandbox.dimensions.commands.autoComplete.DimensionAutoComplete;
import io.sandbox.dimensions.dimension.DimensionSave;
import io.sandbox.dimensions.player.PlayerData;
import io.sandbox.dimensions.player.PlayerPosition;
import net.fabricmc.fabric.api.dimension.v1.FabricDimensions;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;

public class JoinDimension {
  public static LiteralArgumentBuilder<ServerCommandSource> register() {
    return CommandManager.literal("join")
      .then(
        CommandManager.argument("dimension", DimensionArgumentType.dimension())
          .suggests(DimensionAutoComplete.Instance())
          .executes(ctx -> joinDimension(
            DimensionArgumentType.getDimensionArgument(ctx, "dimension"),
            ctx.getSource()
          ))
      )
      .executes(context -> {
        System.out.println("Fallback????");
        return 1;
      });
  }

  private static int joinDimension(ServerWorld dimension, ServerCommandSource source) {
    ServerPlayerEntity player;
    try {
      player = source.getPlayerOrThrow();
    } catch (CommandSyntaxException e) {
      e.printStackTrace();
      return 0;
    }

    // get Overworld to save previous data to
    ServerWorld overworld = source.getServer().getWorld(World.OVERWORLD);
    DimensionSave overworldSaveData = DimensionSave.getDimensionState(overworld);
    PlayerData overworldPlayerData = overworldSaveData.getPlayerData(player);
    PlayerPosition playerPos = new PlayerPosition();
    playerPos.dimension = player.getServerWorld().getRegistryKey().getValue().toString();
    playerPos.posX = player.getBlockX();
    playerPos.posY = player.getBlockY();
    playerPos.posZ = player.getBlockZ();
    
    // Just keep adding pos, when they leave we just pop until nothing is left
    overworldPlayerData.previousPositions.add(playerPos);
    overworldSaveData.setPlayerData(player.getUuid(), overworldPlayerData);
    
    // Get the Dimension Spawn location (where we drop them off when they join)
    DimensionSave dimensionSave = DimensionSave.getDimensionState(dimension);
    BlockPos spawnPos = dimensionSave.getSpawnPos(dimension);
    FabricDimensions.teleport(
      player,
      dimension,
      new TeleportTarget(
        new Vec3d(
          spawnPos.getX() + 0.5,
          spawnPos.getY() + 1,
          spawnPos.getZ() + 0.5
        ),
        new Vec3d(0, 0, 0),
        player.getYaw(),
        player.getPitch()
      )
    );
    System.out.println("Teleported Player: " + player.getName() + " to dimension: " + dimension.getRegistryKey().getValue().toString());
    return 1;
  }
}
