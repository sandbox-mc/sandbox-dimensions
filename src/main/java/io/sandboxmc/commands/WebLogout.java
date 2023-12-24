package io.sandboxmc.commands;

import java.io.IOException;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.sandboxmc.Web;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class WebLogout {
  public static LiteralArgumentBuilder<ServerCommandSource> register() {
    return CommandManager.literal("logout")
      .executes(context -> logoutCmd(context));
  }

  private static int logoutCmd(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerCommandSource source = context.getSource();

    String bearerToken = Web.getBearerToken(source);
    if (bearerToken == null) {
      sendFeedback(source, "Not currently logged in.");
      return 1;
    }

    Web web = new Web(source, "/clients/auth/logout", bearerToken);
    web.setDeleteBody();

    try {
      web.getString(); // we don't even need to do anything with this...
    } catch (IOException e) {
      // I don't think we care?
    } finally {
      web.closeReaders();
    }

    Web.removeBearerToken(source);

    sendFeedback(source, "Logged out of SandboxMC.");

    return 1;
  }

  private static void sendFeedback(ServerCommandSource source, String text) {
    source.sendFeedback(() -> {
      return Text.literal(text);
    }, false);
  }
}
