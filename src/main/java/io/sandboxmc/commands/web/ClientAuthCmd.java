package io.sandboxmc.commands.web;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.sandboxmc.web.ClientAuth;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class ClientAuthCmd {
  public static LiteralArgumentBuilder<ServerCommandSource> register(String commandName) {
    return CommandManager.literal(commandName)
      .then(
        CommandManager.argument("auth-code", StringArgumentType.word())
        .executes(context -> submitAuthCode(context))
      )
      .executes(context -> getAuthToken(context));
  }

  // Very basic wrapper around the thread.
  // This is a blind function and does not know the outcome of the thread at the time of command completion.
  private static int getAuthToken(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    Runnable webAuthThread = new ClientAuth(context, "getAuthToken");
    new Thread(webAuthThread).start();

    return 1;
  }

  // Very basic wrapper around the thread.
  // This is a blind function and does not know the outcome of the thread at the time of command completion.
  private static int submitAuthCode(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    Runnable webAuthThread = new ClientAuth(context, "submitAuthCode");
    new Thread(webAuthThread).start();

    return 1;
  }
}
