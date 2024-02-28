package io.sandboxmc.commands.dimension;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import io.sandboxmc.commands.autoComplete.DimensionAutoComplete;
import io.sandboxmc.dimension.DimensionManager;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class DeleteDimension {
  public static LiteralArgumentBuilder<ServerCommandSource> register() {
    return CommandManager.literal("delete").then(
      CommandManager
      .argument("dimension", IdentifierArgumentType.identifier())
      .suggests(DimensionAutoComplete.Instance())
      .executes(context -> deleteDimension(context))
    ).executes(context -> {
      // No arguments given, do nothing.
      return 0;
    });
  }

  private static int deleteDimension(CommandContext<ServerCommandSource> context) {
    Identifier dimensionIdentifier = IdentifierArgumentType.getIdentifier(context, "dimension");
    ServerCommandSource source = context.getSource();
    MinecraftServer server = source.getServer();

    DimensionManager.deleteDimension(server, dimensionIdentifier);

    source.sendFeedback(() -> {
      return Text.literal("Created new Dimension: " + dimensionIdentifier);
    }, false);
    return 1;
  }
}
