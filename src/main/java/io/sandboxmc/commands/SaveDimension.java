package io.sandboxmc.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import io.sandboxmc.commands.autoComplete.DimensionAutoComplete;
import io.sandboxmc.datapacks.Datapack;
import io.sandboxmc.datapacks.DatapackManager;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

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
    ServerWorld dimension = DimensionArgumentType.getDimensionArgument(context, "dimension");
    Identifier dimensionId = dimension.getRegistryKey().getValue();
    String datapackName = DatapackManager.getDatapackName(dimensionId);
    Datapack datapack = DatapackManager.getDatapack(datapackName);
    datapack.zipWorldfilesToDatapack(dimension);

    // Path tmpZipPath; // This is the path to the .zip file
    // try {
    //   tmpZipPath = datapack.createTmpZip();
    // } catch(IOException ex) {
    //   ex.printStackTrace();
    // }

    context.getSource().sendFeedback(() -> {
      return Text.literal("Saved Dimension: " + dimensionId);
    }, false);

    return 1;
  }
}
