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

public class RemoveDimension {
  public static LiteralArgumentBuilder<ServerCommandSource> register() {
    return CommandManager.literal("removeDimension").then(
      CommandManager.argument("datapackName", StringArgumentType.word())
      .suggests(new StringListAutoComplete(getDatapackNames()))
      .then(
        CommandManager
        .argument("dimension", IdentifierArgumentType.identifier())
        .suggests(new DimensionAutoComplete(getDimensionsInDatapack()))
        .executes(context -> removeDimension(context))
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

  private static Function<CommandContext<ServerCommandSource>, List<Identifier>> getDimensionsInDatapack() {
    return (context) -> {
      String datapackName = StringArgumentType.getString(context, "datapackName");
      Datapack datapack = DatapackManager.getDatapack(datapackName);

      return new ArrayList<>(datapack.getDimensionIds());
    };
  }

  private static int removeDimension(CommandContext<ServerCommandSource> context) {
    ServerCommandSource source = context.getSource();
    String datapackName = StringArgumentType.getString(context, "datapackName");
    Identifier dimensionId = IdentifierArgumentType.getIdentifier(context, "dimension");
    Plunger.info("Setting Dimension: " + dimensionId + " to Datapack: " + datapackName);
    Datapack datapack = DatapackManager.getDatapack(datapackName);
    if (datapack == null) {
      source.sendFeedback(() -> {
        return Text.literal("Datapack does not exist: " + datapackName);
      }, false);
      return 0;
    }
    
    // DimensionSave dimensionSave = DimensionManager.getDimensionSave(dimensionId);
    // if (dimensionSave == null) {
    //   source.sendFeedback(() -> {
    //     return Text.literal("Dimension does not exist: " + dimensionId);
    //   }, false);
    //   return 0;
    // }

    datapack.removeDimension(dimensionId);
    
    source.sendFeedback(() -> {
      return Text.literal("Removing Dimension: " + dimensionId + " from Datapack: " + datapackName);
    }, false);
    return 1;
  }
}
