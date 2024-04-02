package io.sandboxmc.commands.datapack;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import io.sandboxmc.commands.autoComplete.DimensionAutoComplete;
import io.sandboxmc.commands.autoComplete.StringListAutoComplete;
import io.sandboxmc.datapacks.Datapack;
import io.sandboxmc.datapacks.DatapackManager;
import io.sandboxmc.dimension.DimensionManager;
import io.sandboxmc.dimension.DimensionSave;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

public class AddDimension {
  public static LiteralArgumentBuilder<ServerCommandSource> register() {
    return CommandManager.literal("addDimension").then(
      CommandManager.argument("datapackName", StringArgumentType.word())
      .suggests(new StringListAutoComplete(getDatapackNames()))
      .then(
        CommandManager
        .argument("dimension", IdentifierArgumentType.identifier())
        .suggests(new DimensionAutoComplete(getDimensionsNotInDatapack()))
        .then(
          CommandManager.argument("forceMove", BoolArgumentType.bool())
          .executes(context -> addDimension(context, BoolArgumentType.getBool(context, "forceMove"))))
        .executes(context -> addDimension(context, false))
      )
    ).executes(context -> {
      // no arguments given, do nothing
      return 0;
    });
  }

  private static Function<CommandContext<ServerCommandSource>, List<Identifier>> getDimensionsNotInDatapack() {
    return (context) -> {
      String datapackName = StringArgumentType.getString(context, "datapackName");
      Collection<DimensionSave> dimensions = DimensionManager.getDimensions().values();
      ArrayList<Identifier> outputList = new ArrayList<Identifier>();
      dimensions.forEach(dimension -> {
        String parentDatapackName = dimension.getDatapackName();
        if (parentDatapackName == null || !parentDatapackName.equalsIgnoreCase(datapackName)) {
          outputList.add(dimension.getIdentifier());
        }
      });

      return outputList;
    };
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

  private static int addDimension(CommandContext<ServerCommandSource> context, Boolean forceMove) {
    ServerCommandSource source = context.getSource();
    String datapackName = StringArgumentType.getString(context, "datapackName");
    Identifier dimension = IdentifierArgumentType.getIdentifier(context, "dimension");
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

    String currentDatapackName = dimensionSave.getDatapackName();
    if (currentDatapackName != null) {
      if (!forceMove || currentDatapackName.equalsIgnoreCase(datapackName)) {
        if (!currentDatapackName.equalsIgnoreCase(datapackName)) {
          String moveDimensionCmd = "/sbmc datapack addDimension " + datapackName + " " + dimension + " true";
          MutableText feedbackText = Text.literal("Dimension is already part of datapack: " + dimensionSave.getDatapackName() + "\n\n");
          MutableText MoveText = Text.literal("[CLICK HERE TO MOVE DIMENSION]");
          ClickEvent unpackEvent = new ClickEvent(ClickEvent.Action.RUN_COMMAND, moveDimensionCmd);
          MoveText.setStyle(Style.EMPTY.withClickEvent(unpackEvent));
          MoveText.formatted(Formatting.UNDERLINE).formatted(Formatting.BLUE);
          feedbackText.append(MoveText);
          source.sendFeedback(() -> feedbackText, false);
          return 0;
        }

        source.sendFeedback(() -> {
          return Text.literal("Dimension is already part of datapack: " + dimensionSave.getDatapackName());
        }, false);

        return 0;
      }

      // Remove the dimension from it's current datapack and add to new one
      Datapack currentDatapack = DatapackManager.getDatapack(currentDatapackName);
      currentDatapack.removeDimension(dimension);
    }

    datapack.addDimension(dimensionSave);
    
    source.sendFeedback(() -> {
      return Text.literal("Adding Dimension: " + dimension + " to Datapack: " + datapackName);
    }, false);
    return 1;
  }
}
