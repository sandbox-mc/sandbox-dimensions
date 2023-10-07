package io.sandbox.dimensions.commands;

import org.jetbrains.annotations.Nullable;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.sandbox.dimensions.dimension.DimensionSave;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class RestoreCommand {
  public static LiteralArgumentBuilder<ServerCommandSource> register() {
    return CommandManager.literal("restore")
      .then(
        CommandManager.argument("file", StringArgumentType.word())
        .executes(ctx -> execute(
          StringArgumentType.getString(ctx, "file"),
          null,
          ctx.getSource()
        ))
      )
      .executes(context -> {
        System.out.println("Fallback????");
        return 1;
      });
    }
    
  private static int execute(String file, @Nullable String comment, ServerCommandSource source) throws CommandSyntaxException {
    var dimensionSave = new DimensionSave();
    dimensionSave.loadSaveFile(file, source);

    return 1;
  }
}
