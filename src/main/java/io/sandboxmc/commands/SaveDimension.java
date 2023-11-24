package io.sandboxmc.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import io.sandbox.dimensions.dimension.DimensionSave;
import io.sandboxmc.commands.autoComplete.DimensionAutoComplete;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;

public class SaveDimension {
  public static LiteralArgumentBuilder<ServerCommandSource> register() {
    return CommandManager.literal("save")
      .then(
        CommandManager.argument("dimension", DimensionArgumentType.dimension())
          .suggests(DimensionAutoComplete.Instance())
          .executes(ctx -> saveDimension(
            DimensionArgumentType.getDimensionArgument(ctx, "dimension"),
            ctx.getSource()
          ))
      )
      .executes(context -> {
        System.out.println("Fallback????");
        return 1;
      });
  }

  private static int saveDimension(ServerWorld dimension, ServerCommandSource source) {
    DimensionSave.saveDimensionFile(dimension);
    return 1;
  }
}
