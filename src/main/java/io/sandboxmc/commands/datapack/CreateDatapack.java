package io.sandboxmc.commands.datapack;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import io.sandboxmc.Plunger;
import io.sandboxmc.datapacks.Datapack;
import io.sandboxmc.datapacks.DatapackManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class CreateDatapack {
  public static LiteralArgumentBuilder<ServerCommandSource> register() {
    return CommandManager.literal("create").then(
      CommandManager.argument("datapackName", StringArgumentType.word())
      .executes(context -> createDatapack(context))
      .then(
        CommandManager
        .argument("description", StringArgumentType.greedyString())
        .executes(context -> createDatapack(context))
      )
    ).executes(context -> {
      // no arguments given, do nothing
      return 0;
    });
  }

  private static int createDatapack(CommandContext<ServerCommandSource> context) {
    ServerCommandSource source = context.getSource();
    String datapackName = StringArgumentType.getString(context, "datapackName");
    String description = StringArgumentType.getString(context, "description");
    Datapack datapack = DatapackManager.getOrCreateDatapack(datapackName);
    
    if (!description.isEmpty()) {
      // add description if present, it has a default...
      datapack.setDescription(description);
    }

    // Write the base files
    datapack.initializeDatapack();
    source.sendFeedback(() -> {
      return Text.literal("Created Datapack: " + datapackName);
    }, false);
    return 1;
  }
}
