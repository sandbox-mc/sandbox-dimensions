package io.sandboxmc.commands.autoComplete;

import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import io.sandbox.dimensions.dimension.DimensionManager;
import net.minecraft.server.command.ServerCommandSource;

public final class DimensionAutoComplete implements SuggestionProvider<ServerCommandSource> {
  private static final DimensionAutoComplete INSTANCE = new DimensionAutoComplete();

  public static DimensionAutoComplete Instance() { return INSTANCE; }

  @Override
  public CompletableFuture<Suggestions> getSuggestions(
    CommandContext<ServerCommandSource> context,
    SuggestionsBuilder builder
  ) throws CommandSyntaxException {
    String remaining = builder.getRemaining();

    for (String dimensionName : DimensionManager.getDimensionList()) {
      if (dimensionName.startsWith(remaining)) {
        builder.suggest(dimensionName);
      }
    }

    return builder.buildFuture();
  }
}
