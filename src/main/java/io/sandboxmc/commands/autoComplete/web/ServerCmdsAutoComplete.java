package io.sandboxmc.commands.autoComplete.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.server.command.ServerCommandSource;

public class ServerCmdsAutoComplete implements SuggestionProvider<ServerCommandSource> {
  public static ArrayList<String> VALID_COMMANDS = new ArrayList<String>(Arrays.asList("info", "claim", "unclaim", "recover"));

  public ServerCmdsAutoComplete() {}

  @Override
  public CompletableFuture<Suggestions> getSuggestions(
    CommandContext<ServerCommandSource> context,
    SuggestionsBuilder builder
  ) throws CommandSyntaxException {
    String remaining = builder.getRemaining();

    for (String commandName : VALID_COMMANDS) {
      if (commandName.toString().startsWith(remaining)) {
        builder.suggest(commandName.toString());
      }
    }

    return builder.buildFuture();
  }
}
