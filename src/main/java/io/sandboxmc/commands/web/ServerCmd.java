package io.sandboxmc.commands.web;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.sandboxmc.web.Server;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class ServerCmd {
  public static LiteralArgumentBuilder<ServerCommandSource> register(String commandName) {
    return CommandManager.literal(commandName)
      .then(
        CommandManager.argument("claim", StringArgumentType.word())
        .executes(context -> claimServer(context))
      )
      // .then(
      //   CommandManager.argument("unclaim", StringArgumentType.word())
      //   .executes(context -> unclaimServer(context))
      // )
      .executes(context -> serverInfo(context));
  }

  private static int claimServer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    Runnable thread = new Server(context);
    new Thread(thread).start();

    return 1;
  }

  private static int serverInfo(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    Server server = new Server(context);
    server.printInfo();

    return 1;
  }
}
