package io.sandboxmc.commands.dimension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.sandboxmc.commands.autoComplete.DimensionAutoComplete;
import io.sandboxmc.datapacks.Datapack;
import io.sandboxmc.datapacks.DatapackManager;
import io.sandboxmc.dimension.DimensionManager;
import io.sandboxmc.mixin.MinecraftServerAccessor;
import io.sandboxmc.zip.ZipUtility;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.level.storage.LevelStorage.Session;

public class CopyDimension {
  public static LiteralArgumentBuilder<ServerCommandSource> register() {
    return CommandManager.literal("copy")
      .then(
        CommandManager.argument("dimension", DimensionArgumentType.dimension())
        .suggests(DimensionAutoComplete.Instance())
        .then(
          CommandManager
          .argument("namespace", StringArgumentType.word())
          .then(
            CommandManager
            .argument("dimensionName", StringArgumentType.word())
            .executes(context -> copyDimension(context))
          )
        )
      )
      .executes(context -> {
        context.getSource().sendFeedback(() -> {
          return Text.literal("Please select a valid Dimension");
        }, false);
        return 1;
      });
  }

  private static int copyDimension(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    MinecraftServer server = context.getSource().getServer();
    Session session = ((MinecraftServerAccessor)context.getSource().getServer()).getSession();
    ServerWorld dimension = DimensionArgumentType.getDimensionArgument(context, "dimension");
    String namespace = StringArgumentType.getString(context, "namespace");
    String dimensionName = StringArgumentType.getString(context, "dimensionName");
    Identifier dimensionId = dimension.getRegistryKey().getValue();
    Identifier newDimensionId = new Identifier(namespace, dimensionName);
    String rootPath = session.getDirectory(WorldSavePath.ROOT).toString();

    // Get directory to copy
    Path dimensionDirectory = session.getWorldDirectory(dimension.getRegistryKey());

    // Create directory and copy dimensionDirectory there
    File copyTargetDirectory = Paths.get(rootPath, "dimensions", namespace, dimensionName).toFile();
    if (!copyTargetDirectory.exists()) {
      copyTargetDirectory.mkdirs();
    } else {
      // It should not exist...
      System.out.println("Already exists...");
      return 0;
    }

    try {
      ZipUtility.copyDirectory(dimensionDirectory, copyTargetDirectory.toPath(), StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      e.printStackTrace();
      context.getSource().sendFeedback(() -> {
        return Text.literal("Failed to copy Dimension: " + dimensionId);
      }, false);
      return 0;
    }

    // Create world once files are in place using dimension data
    DimensionManager.createDimensionWorld(server, newDimensionId, dimension.getDimensionKey().getValue(), dimension.getSeed());

    context.getSource().sendFeedback(() -> {
      return Text.literal("Copied Dimension: " + dimensionId);
    }, false);

    return 1;
  }
}
