package io.sandboxmc.commands.web;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.sandboxmc.commands.autoComplete.web.ServerCmdsAutoComplete;
import io.sandboxmc.web.Server;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

public class ServerCmd {
  public static LiteralArgumentBuilder<ServerCommandSource> register(String commandName) {
    return CommandManager.literal(commandName)
      .then(
        CommandManager.argument("serverCommand", StringArgumentType.word())
        .suggests(new ServerCmdsAutoComplete())
        .executes(context -> {
          String serverCommand = StringArgumentType.getString(context, "serverCommand");

          if (serverCommand.equals("info")) {
            return serverInfo(context);
          }

          Server server = new Server(context);
          server.runTask(serverCommand);

          return 1;
        })
      )
      .executes(context -> serverInfo(context));
  }

  private static int serverInfo(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    Server server = new Server(context);
    server.printInfo();

    return 1;
  }
}
