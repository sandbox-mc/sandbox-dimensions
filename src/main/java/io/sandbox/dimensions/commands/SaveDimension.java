package io.sandbox.dimensions.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import io.sandbox.dimensions.commands.autoComplete.DimensionAutoComplete;
import io.sandbox.dimensions.dimension.DimensionSave;
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
    DimensionSave.saveDimensionToFile(dimension);
    return 1;
  }
}
