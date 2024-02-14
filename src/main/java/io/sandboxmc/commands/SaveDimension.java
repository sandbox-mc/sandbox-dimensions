package io.sandboxmc.commands;

import java.nio.file.Path;
import java.nio.file.Paths;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.sandboxmc.commands.autoComplete.DimensionAutoComplete;
import io.sandboxmc.datapacks.Datapack;
import io.sandboxmc.datapacks.DatapackManager;
import io.sandboxmc.mixin.MinecraftServerAccessor;
import io.sandboxmc.zip.ZipUtility;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.level.storage.LevelStorage.Session;

public class SaveDimension {
  public static LiteralArgumentBuilder<ServerCommandSource> register() {
    return CommandManager.literal("save")
      .then(
        CommandManager.argument("dimension", DimensionArgumentType.dimension())
          .suggests(DimensionAutoComplete.Instance())
          .executes(context -> saveDimension(context))
      )
      .executes(context -> {
        context.getSource().sendFeedback(() -> {
          return Text.literal("Please select a valid Dimension");
        }, false);
        return 1;
      });
  }

  private static int saveDimension(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    Session session = ((MinecraftServerAccessor)context.getSource().getServer()).getSession();
    Path storageFolderPath = DatapackManager.getStorageFolder(session);

    ServerWorld dimension = DimensionArgumentType.getDimensionArgument(context, "dimension");
    Identifier dimensionId = dimension.getRegistryKey().getValue();
    String datapackName = DatapackManager.getDatapackName(dimensionId);
    Datapack datapack = DatapackManager.getDatapack(datapackName);

    datapack.zipWorldfilesToDatapack(dimension);

    ZipUtility.zipDirectory(datapack.datapackPath.toFile(), Paths.get(storageFolderPath.toString(), datapackName + ".zip").toString());

    context.getSource().sendFeedback(() -> {
      return Text.literal("Saved Dimension: " + dimensionId);
    }, false);

    return 1;
  }
}
