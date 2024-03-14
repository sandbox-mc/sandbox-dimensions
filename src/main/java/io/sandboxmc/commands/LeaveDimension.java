package io.sandboxmc.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.sandboxmc.Plunger;
import io.sandboxmc.dimension.DimensionSave;
import io.sandboxmc.player.PlayerData;
import io.sandboxmc.player.PlayerPosition;
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
      .executes(context -> leaveDimension(context));
  }

  private static int leaveDimension(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerCommandSource source = context.getSource();
    ServerPlayerEntity player = source.getPlayerOrThrow();
    ServerWorld overworld = source.getServer().getWorld(World.OVERWORLD);
    PlayerData overworldPlayerData = DimensionSave.buildDimensionSave(overworld).getPlayerData(player);

    if (overworldPlayerData.previousPositions.size() == 0) {
      player.sendMessage(Text.translatable("sandbox-dimensions.errorMessages.no-previous-pos"), true);
      return 0; // no previous positions
    }

    PlayerPosition playerPos = overworldPlayerData.previousPositions.remove(
      overworldPlayerData.previousPositions.size() - 1
    );
    ServerWorld dimension = source.getServer().getWorld(
      RegistryKey.of(RegistryKeys.WORLD, new Identifier(playerPos.dimension))
    );
    DimensionSave leavingFromDimensionSave = DimensionSave.buildDimensionSave(player.getServerWorld());
    if (!leavingFromDimensionSave.getRule(DimensionSave.KEEP_INVENTORY_ON_JOIN)) {
      // If the world the player is leaving from has KeepInvOnJoin set to false
      // we want to try to give them back their inventory from this previous world
      DimensionSave destinationDimentionSave = DimensionSave.buildDimensionSave(dimension);
      destinationDimentionSave.swapPlayerInventoryWithDestination(player);
    }

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
    Plunger.debug("Teleported Player: " + player.getName() + " to dimension: " + dimension.getRegistryKey().getValue().toString());
    return 1;
  }
}
