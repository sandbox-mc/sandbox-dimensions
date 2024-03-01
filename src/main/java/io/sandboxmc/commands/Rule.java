package io.sandboxmc.commands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.sandboxmc.dimension.DimensionSave;
import io.sandboxmc.commands.autoComplete.DimensionAutoComplete;
import io.sandboxmc.commands.autoComplete.DimensionRulesAutoComplete;
import net.minecraft.command.argument.DimensionArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;

public class Rule {
  public static LiteralArgumentBuilder<ServerCommandSource> register() {
    return CommandManager.literal("rule")
      .then(CommandManager.argument("dimension", DimensionArgumentType.dimension()).suggests(DimensionAutoComplete.Instance())
        .then(CommandManager.argument("dimensionRule", StringArgumentType.word()).suggests(DimensionRulesAutoComplete.Instance())
          .executes(context -> getDimensionRule(context))
          .then(CommandManager.argument("value", BoolArgumentType.bool())
            .executes(context -> setDimensionRule(context))
          )
        )
      )
      // TODO:BRENT add specific blockPos as secondary argument
      .executes(context -> {
        System.out.println("Fallback????");
        return 1;
      });
  }

  private static int getDimensionRule(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    // Get rule
    ServerWorld dimension = DimensionArgumentType.getDimensionArgument(context, "dimension");
    String rule = StringArgumentType.getString(context, "dimensionRule");
    DimensionSave dimensionSave = DimensionSave.buildDimensionSave(dimension);
    Boolean ruleValue = dimensionSave.getRule(rule);
    context.getSource().sendFeedback(() -> {
      return Text.literal("DimensionRule " + rule + " is currently set to: " + ruleValue.toString());
    }, false);
    return 1;
  }

  private static int setDimensionRule(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    // Set dimension rule
    ServerWorld dimension = DimensionArgumentType.getDimensionArgument(context, "dimension");
    String rule = StringArgumentType.getString(context, "dimensionRule");
    Boolean value = BoolArgumentType.getBool(context, "value");

    DimensionSave dimensionSave = DimensionSave.buildDimensionSave(dimension);
    dimensionSave.setRule(rule, value);
    context.getSource().sendFeedback(() -> {
      return Text.literal("DimensionRule " + rule + " has been set to: " + value.toString());
    }, false);
    return 1;
  }
}
