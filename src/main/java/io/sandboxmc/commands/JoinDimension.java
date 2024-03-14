package io.sandboxmc.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.sandboxmc.dimension.DimensionSave;
import io.sandboxmc.player.PlayerData;
import io.sandboxmc.player.PlayerPosition;
import io.sandboxmc.Plunger;
import io.sandboxmc.commands.autoComplete.DimensionAutoComplete;
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
          .executes(context -> joinDimension(context))
      )
      .executes(context -> {
        Plunger.debug("Fallback????");
        return 1;
      });
  }

  // private static Function<CommandContext<ServerCommandSource>, List<String>> getNamespaceAutoCompleteOptions() {
  //   return (context) -> {
  //     // Get previous argument to build the path to namespace inside datapack
  //     String datapackName = context.getArgument("datapack", String.class);
  //     Session session = ((MinecraftServerAccessor)(context.getSource().getServer())).getSession();
  //     Path datapackPath = session.getDirectory(WorldSavePath.DATAPACKS);
  //     File datapackDataDirectory = Paths.get(datapackPath.toString(), datapackName, "data").toFile();
  //     List<String> namespaceFolderList = new ArrayList<>();

  //     if (datapackDataDirectory.exists()) {
  //       File[] folderList = datapackDataDirectory.listFiles((dir, name) -> dir.isDirectory());
    
  //       for (File datapackDir : folderList) {
  //         // get the folder name
  //         namespaceFolderList.add(datapackDir.getName());
  //       }
  //     }

  //     return namespaceFolderList;
  //   };
  // }

  private static int joinDimension(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerCommandSource source = context.getSource();
    ServerWorld dimension = DimensionArgumentType.getDimensionArgument(context, "dimension");
    ServerPlayerEntity player;
    try {
      player = source.getPlayerOrThrow();
    } catch (CommandSyntaxException e) {
      e.printStackTrace();
      return 0;
    }

    // get Overworld to save previous data to
    ServerWorld overworld = source.getServer().getWorld(World.OVERWORLD);
    DimensionSave overworldSaveData = DimensionSave.buildDimensionSave(overworld);
    PlayerData overworldPlayerData = overworldSaveData.getPlayerData(player);
    ServerWorld originalDimension = player.getServerWorld();
    PlayerPosition playerPos = new PlayerPosition();
    playerPos.dimension = originalDimension.getRegistryKey().getValue().toString();
    playerPos.posX = player.getBlockX();
    playerPos.posY = player.getBlockY();
    playerPos.posZ = player.getBlockZ();
    
    // Just keep adding pos, when they leave we just pop until nothing is left
    overworldPlayerData.previousPositions.add(playerPos);
    overworldSaveData.setPlayerData(player.getUuid(), overworldPlayerData);
    
    DimensionSave dimensionSave = DimensionSave.buildDimensionSave(dimension);

    // swap inventory if keepInventoryOnJoin rule is false
    if (!dimensionSave.getRule(DimensionSave.KEEP_INVENTORY_ON_JOIN)) {
      PlayerData playerData = dimensionSave.getPlayerData(player);
      playerData.previousPositions.add(playerPos);
      dimensionSave.swapPlayerInventoryWithDestination(player);
    }

    // Get the Dimension Spawn location (where we drop them off when they join)
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
    Plunger.debug("Teleported Player: " + player.getName() + " to dimension: " + dimension.getRegistryKey().getValue().toString());
    return 1;
  }
}
