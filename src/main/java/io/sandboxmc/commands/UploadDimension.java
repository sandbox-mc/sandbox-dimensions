package io.sandboxmc.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.sandboxmc.commands.autoComplete.WebAutoComplete;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class UploadDimension {
  public static LiteralArgumentBuilder<ServerCommandSource> register() {
    return CommandManager.literal("upload")
      .then(
        CommandManager.argument("creator", StringArgumentType.word())
        .suggests(new WebAutoComplete("creators"))
        .then(
          CommandManager.argument("dimension", StringArgumentType.word())
          .suggests(new WebAutoComplete("dimensions", "creators", "creator"))
          .then(
            CommandManager.argument("version", StringArgumentType.word())
            .executes(context -> performUploadCmd(context))
          )
          .executes(context -> performUploadCmd(context))
        )
        .executes(context -> {
          sendFeedback(context.getSource(), Text.literal("No dimension given."));
          return 0;
        })
      )
      .executes(context -> {
        sendFeedback(context.getSource(), Text.literal("No creator or dimension given."));
        return 0;
      });
  }

  private static int performUploadCmd(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    
    return 1;
  }

  // TODO: Pull this into a more globally available helper...
  private static void sendFeedback(ServerCommandSource source, Text feedbackText) {
    source.sendFeedback(() -> {
      return feedbackText;
    }, false);
  }
}
