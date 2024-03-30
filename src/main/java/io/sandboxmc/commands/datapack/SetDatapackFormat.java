package io.sandboxmc.commands.datapack;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class SetDatapackFormat {
  public static LiteralArgumentBuilder<ServerCommandSource> register() {
    return CommandManager.literal("setFormat").then(
      CommandManager
      .argument("format", IntegerArgumentType.integer())
      .executes(context -> installDatapack(context))
    ).executes(context -> {
      // no arguments given, do nothing
      return 0;
    });
  }

  private static int installDatapack(CommandContext<ServerCommandSource> context) {
    ServerCommandSource source = context.getSource();
    Integer format = IntegerArgumentType.getInteger(context, "format");

    source.sendFeedback(() -> {
      return Text.literal("Set datapack format to: " + format);
    }, false);
    return 1;
  }
}
