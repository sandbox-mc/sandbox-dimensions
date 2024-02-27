package io.sandboxmc.commands.autoComplete.web;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.concurrent.CompletableFuture;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.LiteralMessage;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.server.command.ServerCommandSource;

public class WebAutoComplete implements SuggestionProvider<ServerCommandSource> {

  public static ArrayList<String> VALID_TYPES = new ArrayList<String>(Arrays.asList("creators", "dimensions"));
  public static String PREFIX_REPLACE_VAL = "__PREFIX_VALUE__";
  public static String URL_REGEXP = "^[\\w-]+$";
  private String urlPart = null;
  private String urlPrefixValue = "";
  private Boolean restrictToAuth = false;

  public WebAutoComplete(String autocompleteType) {
    if (VALID_TYPES.indexOf(autocompleteType) != -1) {
      urlPart = autocompleteType;
    }
  }

  public WebAutoComplete(String autocompleteType, Boolean onlyAuthed) {
    this(autocompleteType);

    restrictToAuth = onlyAuthed;
  }

  public WebAutoComplete(String autocompleteType, String prefixType, String prefixValue) {
    this(autocompleteType);

    if (VALID_TYPES.indexOf(prefixType) != -1 && prefixValue.matches(URL_REGEXP)) {
      urlPart = prefixType + "/" + PREFIX_REPLACE_VAL + "/" + urlPart;
      urlPrefixValue = prefixValue;
    }
  }

  public WebAutoComplete(String autocompleteType, String prefixType, String prefixValue, Boolean onlyAuthed) {
    this(autocompleteType, prefixType, prefixValue);
  
    restrictToAuth = onlyAuthed;
  }

  @Override
  public CompletableFuture<Suggestions> getSuggestions(
    CommandContext<ServerCommandSource> context,
    SuggestionsBuilder builder
  ) throws CommandSyntaxException {
    if (urlPart.isBlank()) {
      // Somehow we got a value we weren't expecting in the constructor!
      return builder.buildFuture();
    }

    String remaining = builder.getRemaining();

    if (remaining.length() == 0 || !remaining.matches(URL_REGEXP)) {
      return builder.buildFuture();
    }

    String path = buildPath(context, remaining);

    WebAutoCompleteWorker thread = new WebAutoCompleteWorker(context.getSource(), path, restrictToAuth);
    ArrayList<HashMap<String, String>> valuesToSuggest = thread.getFromCache();
    if (valuesToSuggest == null) {
      // No value found in cache, let's start the worker and return nothing.
      new Thread(thread).start();
      return builder.buildFuture();
    }
    
    valuesToSuggest.forEach((valueToSuggest) -> {
      builder.suggest(valueToSuggest.get("value"), new LiteralMessage(valueToSuggest.get("label")));
    });

    return builder.buildFuture();
  }

  private String buildPath(CommandContext<ServerCommandSource> context, String remaining) {
    String prefixVal = "";
    if (!urlPrefixValue.isBlank()) {
      prefixVal = StringArgumentType.getString(context, urlPrefixValue);
    }
    String replacedUrl = urlPart.replaceFirst(PREFIX_REPLACE_VAL, prefixVal);
    return "/autocomplete/" + replacedUrl + "?q=" + remaining;
  }
}
