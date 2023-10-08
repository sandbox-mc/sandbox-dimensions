package io.sandbox.dimensions.commands.autoComplete;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
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
    Set<String> list = DimensionManager.getDimensionList();
    List<String> output = new ArrayList();

    for (String dimensionName : list) {
      if (dimensionName.startsWith(remaining)) {
        builder.suggest(dimensionName);
      }
    }
    
    System.out.println("Remaining: " + remaining);
    return builder.buildFuture();
  }
  
}
