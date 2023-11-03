package io.sandbox.dimensions.commands.autoComplete;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
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

public class NamespaceAutoComplete implements SuggestionProvider<ServerCommandSource> {
  private static final NamespaceAutoComplete INSTANCE = new NamespaceAutoComplete();

  public static NamespaceAutoComplete Instance() {
    INSTANCE.shouldRefreshCache = true;
    return INSTANCE;
  }

  private Boolean shouldRefreshCache = true;
  private List<String> namespaceFolderList = new ArrayList<>();

  private void refreshCache(CommandContext<ServerCommandSource> context) {
    // Get previous argument to build the path to namespace inside datapack
    String datapackName = context.getArgument("datapack", String.class);
    Session session = ((MinecraftServerAccessor)(context.getSource().getServer())).getSession();
    Path datapackPath = session.getDirectory(WorldSavePath.DATAPACKS);
    File datapackDataDirectory = Paths.get(datapackPath.toString(), datapackName, "data").toFile();
    // Clear out previous list
    this.namespaceFolderList.clear();
    if (datapackDataDirectory.exists()) {
      File[] folderList = datapackDataDirectory.listFiles((dir, name) -> dir.isDirectory());
  
      for (File datapackDir : folderList) {
        // get the folder name
        this.namespaceFolderList.add(datapackDir.getName());
      }
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

    for (String namespaceDirectory : this.namespaceFolderList) {
      if (namespaceDirectory.startsWith(remaining)) {
        builder.suggest(namespaceDirectory);
      }
    }

    return builder.buildFuture();
  }
}
