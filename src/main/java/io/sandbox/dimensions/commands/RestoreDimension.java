package io.sandbox.dimensions.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.sandbox.dimensions.commands.autoComplete.DimensionAutoComplete;
import io.sandbox.dimensions.dimension.DimensionSave;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class RestoreDimension {
  public static LiteralArgumentBuilder<ServerCommandSource> register() {
    return CommandManager.literal("restore")
      .then(
        CommandManager.argument("dimension", DimensionArgumentType.dimension())
          .suggests(DimensionAutoComplete.Instance())
          .executes(ctx -> execute(
            StringArgumentType.getString(ctx, "dimension"),
            ctx.getSource()
          ))
      )
      .executes(context -> {
        System.out.println("Fallback????");
        return 1;
      });
  }
    
  private static int execute(String dimension, ServerCommandSource source) throws CommandSyntaxException {
    var dimensionSave = new DimensionSave();
    System.out.println("Restore Command not fully Implemented yet...");
    // dimensionSave.loadSaveFile(dimension, source.getServer(), true);

    return 1;
  }
}
