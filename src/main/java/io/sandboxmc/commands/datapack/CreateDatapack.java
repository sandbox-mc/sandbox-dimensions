package io.sandboxmc.commands.datapack;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import io.sandboxmc.datapacks.DatapackManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class CreateDatapack {
  public static LiteralArgumentBuilder<ServerCommandSource> register() {
    return CommandManager.literal("create").then(
      CommandManager.argument("datapackName", StringArgumentType.word())
      .executes(context -> createDatapack(context))
    ).executes(context -> {
      // no arguments given, do nothing
      return 0;
    });
  }

  private static int createDatapack(CommandContext<ServerCommandSource> context) {
    ServerCommandSource source = context.getSource();
    String datapackName = StringArgumentType.getString(context, "datapackName");
    DatapackManager.createDatapack(datapackName);

    source.sendFeedback(() -> {
      return Text.literal("Created Datapack: " + datapackName);
    }, false);
    return 1;
  }
}
