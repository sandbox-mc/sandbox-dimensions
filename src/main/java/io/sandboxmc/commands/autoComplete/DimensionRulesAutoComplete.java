package io.sandboxmc.commands.autoComplete;

import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import io.sandbox.dimensions.dimension.DimensionSave;
import net.minecraft.server.command.ServerCommandSource;

public class DimensionRulesAutoComplete implements SuggestionProvider<ServerCommandSource> {
  private static final DimensionRulesAutoComplete INSTANCE = new DimensionRulesAutoComplete();

  public static DimensionRulesAutoComplete Instance() { return INSTANCE; }

  @Override
  public CompletableFuture<Suggestions> getSuggestions(
    CommandContext<ServerCommandSource> context,
    SuggestionsBuilder builder
  ) throws CommandSyntaxException {
    String remaining = builder.getRemaining();

    for (String ruleName : DimensionSave.GAME_RULES) {
      if (ruleName.startsWith(remaining)) {
        builder.suggest(ruleName);
      }
    }

    return builder.buildFuture();
  }
}
