package io.sandboxmc.commands.autoComplete;

import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import io.sandboxmc.dimension.DimensionManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.Identifier;

public final class DimensionAutoComplete implements SuggestionProvider<ServerCommandSource> {
  private static final DimensionAutoComplete INSTANCE = new DimensionAutoComplete();

  public static DimensionAutoComplete Instance() { return INSTANCE; }

  @Override
  public CompletableFuture<Suggestions> getSuggestions(
    CommandContext<ServerCommandSource> context,
    SuggestionsBuilder builder
  ) throws CommandSyntaxException {
    String remaining = builder.getRemaining();

    for (Identifier dimensionName : DimensionManager.getDimensionList()) {
      if (dimensionName.toString().startsWith(remaining)) {
        builder.suggest(dimensionName.toString());
      }
    }

    return builder.buildFuture();
  }
}
