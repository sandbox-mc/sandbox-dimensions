package io.sandboxmc.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.sandbox.dimensions.dimension.DimensionSave;
import io.sandboxmc.commands.autoComplete.DimensionAutoComplete;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;

public class RestoreDimension {
  public static LiteralArgumentBuilder<ServerCommandSource> register() {
    return CommandManager.literal("restore")
      .then(
        CommandManager.argument("dimension", DimensionArgumentType.dimension())
          .suggests(DimensionAutoComplete.Instance())
          .executes(ctx -> execute(
            DimensionArgumentType.getDimensionArgument(ctx, "dimension"),
            ctx.getSource()
          ))
      )
      .executes(context -> {
        System.out.println("Fallback????");
        return 1;
      });
  }
    
  private static int execute(ServerWorld dimension, ServerCommandSource source) throws CommandSyntaxException {
    var dimensionSave = new DimensionSave();
    System.out.println("Restore Command not fully Implemented yet...");
    // Forces datapack save zip to overwrite the current dimension save files
    dimensionSave.dimensionSaveLoaded = DimensionSave.loadDimensionFile(
      dimension.getRegistryKey().getValue().toString(), source.getServer());

    return 1;
  }
}
