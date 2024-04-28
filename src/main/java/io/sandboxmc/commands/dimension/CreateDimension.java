package io.sandboxmc.commands.dimension;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import io.sandboxmc.dimension.DimensionManager;
import io.sandboxmc.dimension.DimensionSave;
import io.sandboxmc.dimension.SandboxWorldConfig;
import io.sandboxmc.commands.autoComplete.StringListAutoComplete;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionOptions;

public class CreateDimension {
  public static LiteralArgumentBuilder<ServerCommandSource> register() {
    return CommandManager.literal("create").then(
      CommandManager
      .argument("namespace", StringArgumentType.word())
      .then(
        CommandManager
        .argument("dimensionName", StringArgumentType.word())
        .then(
          CommandManager
          .argument("dimensionOptionsId", IdentifierArgumentType.identifier())
          .suggests(new StringListAutoComplete(getDimensionAutoCompleteOptions()))
          .then(
            CommandManager
            .argument("seed", LongArgumentType.longArg())
            .executes(context -> createDimension(context, LongArgumentType.getLong(context, "seed"), true))
          ).executes(context -> createDimension(context, 0, false))
        ).executes(context -> createDimension(context, 0, false))
      )
    ).executes(context -> {
      // No arguments given, do nothing.
      return 0;
    });
  }

  private static Function<CommandContext<ServerCommandSource>, List<String>> getDimensionAutoCompleteOptions() {
    return (context) -> {
      Set<RegistryKey<DimensionOptions>> dimensionOptionTypes = context.getSource().getServer()
        .getRegistryManager().get(RegistryKeys.DIMENSION).getKeys();
      List<String> dimensionTypeList = new ArrayList<>();

      for (RegistryKey<DimensionOptions> dimensionOptions : dimensionOptionTypes) {
        dimensionTypeList.add(dimensionOptions.getValue().toString());
      }

      return dimensionTypeList;
    };
  }

  private static int createDimension(CommandContext<ServerCommandSource> context, long seed, Boolean passedSeed) {
    String namespace = StringArgumentType.getString(context, "namespace");
    String dimensionName = StringArgumentType.getString(context, "dimensionName");
    Identifier dimensionOptionsId = IdentifierArgumentType.getIdentifier(context, "dimensionOptionsId");
    Identifier dimensionIdentifier = new Identifier(namespace, dimensionName);
    ServerCommandSource source = context.getSource();
    MinecraftServer server = source.getServer();
    
    
    // Create the dimension
    SandboxWorldConfig config = new SandboxWorldConfig(server);
    config.setSeed(passedSeed ? seed : server.getWorld(World.OVERWORLD).getSeed());
    config.setDimensionOptionsId(dimensionOptionsId);
    DimensionSave dimensionSave = DimensionManager.buildDimensionSaveFromConfig(dimensionIdentifier, config);
    dimensionSave.generateConfigFiles();

    source.sendFeedback(() -> {
      return Text.literal("Created new Dimension: " + dimensionName);
    }, false);
    return 1;
  }
}
