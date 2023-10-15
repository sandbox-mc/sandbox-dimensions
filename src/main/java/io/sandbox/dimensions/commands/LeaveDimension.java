package io.sandbox.dimensions.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.sandbox.dimensions.dimension.DimensionSave;
import io.sandbox.dimensions.player.PlayerData;
import io.sandbox.dimensions.player.PlayerPosition;
import net.fabricmc.fabric.api.dimension.v1.FabricDimensions;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.TeleportTarget;
import net.minecraft.world.World;

public class LeaveDimension {
  public static LiteralArgumentBuilder<ServerCommandSource> register() {
    return CommandManager.literal("leave")
      .executes(ctx -> {
        return leaveDimension(ctx.getSource());
      });
  }

  private static int leaveDimension(ServerCommandSource source) throws CommandSyntaxException {
    ServerPlayerEntity player = source.getPlayerOrThrow();
    ServerWorld overworld = source.getServer().getWorld(World.OVERWORLD);
    PlayerData overworldPlayerData = DimensionSave.getDimensionState(overworld).getPlayerData(player);

    if (overworldPlayerData.previousPositions.size() == 0) {
      player.sendMessage(Text.translatable("sandbox-dimensions.errorMessages.no-previous-pos"), true);
      return 0; // no previous positions
    }

    PlayerPosition playerPos = overworldPlayerData.previousPositions.remove(
      overworldPlayerData.previousPositions.size() - 1
    );
    ServerWorld dimension = source.getServer().getWorld(RegistryKey.of(RegistryKeys.WORLD, new Identifier(playerPos.dimension)));

    FabricDimensions.teleport(
      player,
      dimension,
      new TeleportTarget(
        new Vec3d(
          playerPos.posX + 0.5,
          playerPos.posY + 1,
          playerPos.posZ + 0.5
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
