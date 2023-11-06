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

import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.world.dimension.DimensionType;

public class DimensionTypeAutoComplete implements SuggestionProvider<ServerCommandSource> {
  private static final DimensionTypeAutoComplete INSTANCE = new DimensionTypeAutoComplete();

  public static DimensionTypeAutoComplete Instance() {
    INSTANCE.shouldRefreshCache = true;
    return INSTANCE;
  }

  private Boolean shouldRefreshCache = true;
  private List<String> dimensionTypeList = new ArrayList<>();

  private void refreshCache(CommandContext<ServerCommandSource> context) {
    this.dimensionTypeList.clear();
    Set<RegistryKey<DimensionType>> dimensionTypes = context.getSource().getServer()
      .getRegistryManager().get(RegistryKeys.DIMENSION_TYPE).getKeys();
    for (RegistryKey<DimensionType> dimensionType : dimensionTypes) {
      this.dimensionTypeList.add(dimensionType.getValue().toString());    
    }

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

    for (String dimensionType : this.dimensionTypeList) {
      if (dimensionType.startsWith(remaining)) {
        builder.suggest(dimensionType);
      }
    }

    return builder.buildFuture();
  }
}
