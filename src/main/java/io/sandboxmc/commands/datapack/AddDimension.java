package io.sandboxmc.commands.datapack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import io.sandboxmc.Plunger;
import io.sandboxmc.commands.autoComplete.DimensionAutoComplete;
import io.sandboxmc.commands.autoComplete.StringListAutoComplete;
import io.sandboxmc.datapacks.Datapack;
import io.sandboxmc.datapacks.DatapackManager;
import io.sandboxmc.dimension.DimensionManager;
import io.sandboxmc.dimension.DimensionSave;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class AddDimension {
  public static LiteralArgumentBuilder<ServerCommandSource> register() {
    return CommandManager.literal("addDimension").then(
      CommandManager.argument("datapackName", StringArgumentType.word())
      .suggests(new StringListAutoComplete(getDatapackNames()))
      .then(
        CommandManager
        .argument("dimension", IdentifierArgumentType.identifier())
        .suggests(DimensionAutoComplete.Instance())
        .executes(context -> addDimension(context))
      )
    ).executes(context -> {
      // no arguments given, do nothing
      return 0;
    });
  }

  private static Function<CommandContext<ServerCommandSource>, List<String>> getDatapackNames() {
    return (context) -> {
      List<String> output = new ArrayList<>();
      for (String keyEntry : DatapackManager.getDatapackNames()) {
        output.add(keyEntry);
      }

      return output;
    };
  }

  // private static Function<CommandContext<ServerCommandSource>, List<String>> getDimensions() {
  //   return (context) -> {
  //     List<String> output = new ArrayList<>();
  //     for (Identifier dimensionIdentifier : DimensionManager.getDimensionList()) {
  //       output.add(dimensionIdentifier.toString());
  //     }

  //     return output;
  //   };
  // }

  private static int addDimension(CommandContext<ServerCommandSource> context) {
    ServerCommandSource source = context.getSource();
    String datapackName = StringArgumentType.getString(context, "datapackName");
    Identifier dimension = IdentifierArgumentType.getIdentifier(context, "dimension");
    Plunger.info("Setting Dimension: " + dimension + " to Datapack: " + datapackName);
    Datapack datapack = DatapackManager.getDatapack(datapackName);
    if (datapack == null) {
      source.sendFeedback(() -> {
        return Text.literal("Datapack does not exist: " + datapackName);
      }, false);
      return 0;
    }
    
    DimensionSave dimensionSave = DimensionManager.getDimensionSave(dimension);
    if (dimensionSave == null) {
      source.sendFeedback(() -> {
        return Text.literal("Dimension does not exist: " + dimension);
      }, false);
      return 0;
    }

    datapack.addDimension(dimensionSave);
    
    source.sendFeedback(() -> {
      return Text.literal("Adding Dimension: " + dimension + " to Datapack: " + datapackName);
    }, false);
    return 1;
  }
}
