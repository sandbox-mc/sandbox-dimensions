package io.sandboxmc.commands.web;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.sandboxmc.web.ClientLogout;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class ClientLogoutCmd {
  public static LiteralArgumentBuilder<ServerCommandSource> register() {
    return CommandManager.literal("logout")
      .executes(context -> logoutCmd(context));
  }

  private static int logoutCmd(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    Runnable logoutThread = new ClientLogout(context);
    new Thread(logoutThread).start();

    return 1;
  }
}
