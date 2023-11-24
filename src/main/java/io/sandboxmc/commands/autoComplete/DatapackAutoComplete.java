package io.sandboxmc.commands.autoComplete;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import io.sandbox.dimensions.mixin.MinecraftServerAccessor;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.level.storage.LevelStorage.Session;

public class DatapackAutoComplete implements SuggestionProvider<ServerCommandSource> {
  private static final DatapackAutoComplete INSTANCE = new DatapackAutoComplete();
  public static DatapackAutoComplete Instance() {
    INSTANCE.shouldRefreshCache = true;
    return INSTANCE;
  }

  private Boolean shouldRefreshCache = true;
  private List<String> datapackDirectoryList = new ArrayList<>();

  private void refreshCache(CommandContext<ServerCommandSource> context) {
    Session session = ((MinecraftServerAccessor)(context.getSource().getServer())).getSession();
    Path datapackPath = session.getDirectory(WorldSavePath.DATAPACKS);
    File[] folderList = datapackPath.toFile().listFiles((dir, name) -> dir.isDirectory());

    // Clear out previous list
    this.datapackDirectoryList.clear();
    for (File datapackDir : folderList) {
      this.datapackDirectoryList.add(datapackDir.getName());
    }

    this.shouldRefreshCache = false;
  }

  @Override
  public CompletableFuture<Suggestions> getSuggestions(
    CommandContext<ServerCommandSource> context,
    SuggestionsBuilder builder
  ) throws CommandSyntaxException {
    String remaining = builder.getRemaining();

    // Only get the folders on initialize
    // This should get refreshed whenever commands are reloaded
    // So multiple char typing should not trigger this more than once
    if (shouldRefreshCache) {
      this.refreshCache(context);
    }

    for (String dataPackName : datapackDirectoryList) {
      if (dataPackName.startsWith(remaining)) {
        builder.suggest(dataPackName);
      }
    }

    return builder.buildFuture();
  }
}
