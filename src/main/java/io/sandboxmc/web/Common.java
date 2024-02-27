package io.sandboxmc.web;

import com.mojang.brigadier.context.CommandContext;

import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public class Common {
  protected CommandContext<ServerCommandSource> context;
  protected ServerCommandSource source = null;

  public Common() {}

  public Common(CommandContext<ServerCommandSource> theContext) {
    context = theContext;
    source = context.getSource();
  }

  protected void printMessage(MutableText message) {
    if (source == null) {
      return;
    }

    source.sendFeedback(() -> {
      return message;
    }, false);
  }

  protected void printMessage(String message) {
    printMessage(Text.literal(message));
  }
}
