package io.sandbox.dimensions.commands;

import java.util.Set;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.sandbox.dimensions.dimension.DimensionManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class ListDimensions {
  public static LiteralArgumentBuilder<ServerCommandSource> register() {
    return CommandManager.literal("list")
      .executes(context -> listDimensions(context.getSource()));
  }

  private static int listDimensions(ServerCommandSource source) throws CommandSyntaxException {
    Set<String> dimensionList = DimensionManager.getDimensionList();
    for (String dimensionName : dimensionList) {
      source.sendFeedback(() -> {
        return Text.literal(dimensionName);
      }, false);
    }
    return 1;
  }
}
