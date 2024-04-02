package io.sandboxmc.commands.autoComplete;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import io.sandboxmc.dimension.DimensionManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Identifier;

public class DimensionAutoComplete implements SuggestionProvider<ServerCommandSource> {
  private static final DimensionAutoComplete INSTANCE = new DimensionAutoComplete();

  public static DimensionAutoComplete Instance() { return INSTANCE; }

  // Grab the INSTANCE if you just want everything
  public DimensionAutoComplete(){}

  public DimensionAutoComplete(Function<CommandContext<ServerCommandSource>, List<Identifier>> buildCache) {
    this.buildCache = buildCache;
  }

  Boolean shouldRefreshCache = true;
  private List<Identifier> identifierList = new ArrayList<>();
  private Function<CommandContext<ServerCommandSource>, List<Identifier>> buildCache = (context) -> {
    return new ArrayList<>(DimensionManager.getDimensionList());
  };

  private void refreshCache(CommandContext<ServerCommandSource> context) {
    identifierList = this.buildCache.apply(context);
    // this.shouldRefreshCache = false;
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

    for (Identifier dimensionName : this.identifierList) {
      if (dimensionName.toString().startsWith(remaining)) {
        builder.suggest(dimensionName.toString());
      }
    }

    return builder.buildFuture();
  }
}
