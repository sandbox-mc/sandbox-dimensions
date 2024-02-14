package io.sandboxmc.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.sandboxmc.dimension.DimensionSave;
import io.sandboxmc.commands.autoComplete.DimensionAutoComplete;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public class SetSpawnDimension {
  public static LiteralArgumentBuilder<ServerCommandSource> register() {
    return CommandManager.literal("setspawn")
      .then(
        CommandManager.argument("dimension", DimensionArgumentType.dimension())
          .suggests(DimensionAutoComplete.Instance())
          .executes(context -> setSpawnDimensionPlayerPos(context))
      )
      // TODO: add specific blockPos as secondary argument
      .executes(context -> {
        System.out.println("Fallback????");
        return 1;
      });
  }

  private static int setSpawnDimensionPlayerPos(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerWorld dimension = DimensionArgumentType.getDimensionArgument(context, "dimension");
    // Set the spawn location of dimension
    ServerPlayerEntity player = context.getSource().getPlayerOrThrow();
    DimensionSave dimensionSave = DimensionSave.buildDimensionSave(dimension);
    dimensionSave.setSpawnPos(dimension, player.getBlockPos());
    return 1;
  }
}
