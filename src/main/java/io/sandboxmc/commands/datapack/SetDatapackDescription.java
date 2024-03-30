package io.sandboxmc.commands.datapack;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class SetDatapackDescription {
  public static LiteralArgumentBuilder<ServerCommandSource> register() {
    return CommandManager.literal("setDescription").then(
      CommandManager
      .argument("description", StringArgumentType.greedyString()) // greedy grabs everything after previous arg, must be last arg
      .executes(context -> installDatapack(context))
    ).executes(context -> {
      // no arguments given, do nothing
      return 0;
    });
  }

  private static int installDatapack(CommandContext<ServerCommandSource> context) {
    ServerCommandSource source = context.getSource();
    String description = StringArgumentType.getString(context, "description");

    source.sendFeedback(() -> {
      return Text.literal("Set datapack description to: " + description);
    }, false);
    return 1;
  }
}
