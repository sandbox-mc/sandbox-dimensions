package io.sandbox.dimensions.commands.autoComplete;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.server.command.ServerCommandSource;

public class StringListAutoComplete implements SuggestionProvider<ServerCommandSource> {
  public StringListAutoComplete(Function<CommandContext<ServerCommandSource>, List<String>> buildCache) {
    this.buildCache = buildCache;
  }

  Boolean shouldRefreshCache = true;
  private List<String> stringList = new ArrayList<>();
  private Function<CommandContext<ServerCommandSource>, List<String>> buildCache = (context) -> {
    return this.stringList;
  };

  private void refreshCache(CommandContext<ServerCommandSource> context) {
    stringList = this.buildCache.apply(context);
    this.shouldRefreshCache = false;
  }

  @Override
  public CompletableFuture<Suggestions> getSuggestions(
    CommandContext<ServerCommandSource> context,
    SuggestionsBuilder builder
  ) throws CommandSyntaxException {
    String remaining = builder.getRemaining();
    if (shouldRefreshCache) {
      this.refreshCache(context);
    }

    for (String stringValue : this.stringList) {
      if (stringValue.startsWith(remaining)) {
        builder.suggest(stringValue);
      }
    }

    return builder.buildFuture();
  }
}
