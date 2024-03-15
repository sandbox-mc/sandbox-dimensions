package io.sandboxmc.commands.autoComplete.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import com.google.gson.stream.JsonReader;

import io.sandboxmc.Plunger;
import io.sandboxmc.Web;
import io.sandboxmc.web.Common;
import net.minecraft.server.command.ServerCommandSource;

public class WebAutoCompleteWorker extends Common implements Runnable {
  private static HashMap<String, ArrayList<HashMap<String, String>>> cache = new HashMap<>();
  private static HashMap<String, Integer> playersFetching = new HashMap<>();

  private ServerCommandSource source;
  private String playerUUID;
  private String cacheKey;
  private String pathToFetch;
  private Boolean restrictToAuth = false;

  public WebAutoCompleteWorker(ServerCommandSource theSource, String thePath, Boolean doRestrictToAuth) {
    source = theSource;
    playerUUID = source.getPlayer().getUuidAsString();
    pathToFetch = thePath;
    restrictToAuth = doRestrictToAuth;
    // TODO:TYLER determine cache expiry method
    cacheKey = playerUUID + "|||" + restrictToAuth + "|||" + thePath;
  }

  public ArrayList<HashMap<String, String>> getFromCache() {
    if (cache.containsKey(cacheKey)) {
      return cache.get(cacheKey);
    } else {
      return null;
    }
  }

  public void startThread() {
    Integer numActiveCalls = playersFetching.getOrDefault(playerUUID, 0);
    // Allow up to two calls per person.
    if (numActiveCalls > 1) {
      Plunger.debug("Too many in-flight web autocompletes for user: " + numActiveCalls);
      return;
    }

    // Increment fetch counter
    playersFetching.put(playerUUID, numActiveCalls + 1);

    new Thread(this).start();
  }

  private void decrementCounter() {
    Integer numActiveCalls = playersFetching.getOrDefault(playerUUID, 0);
    if (numActiveCalls > 0) {
      playersFetching.put(playerUUID, numActiveCalls - 1);
    }
  }

  public void run() {
    Web web = new Web(source, pathToFetch, restrictToAuth);

    if (restrictToAuth && !web.hasAuth()) {
      decrementCounter();
      return;
    }

    ArrayList<HashMap<String, String>> valuesToSuggest = new ArrayList<>();
    try {
      readJSON(web.getJson(), valuesToSuggest);
      cache.put(cacheKey, valuesToSuggest);
    } catch (IOException e) {
      Plunger.error("Failed in WebAutoCompleteWorker", e);
    } finally {
      decrementCounter();
      web.closeReaders();
    }
  }

  private void readJSON(JsonReader jsonReader, ArrayList<HashMap<String, String>> valuesToSuggest) throws IOException {
    jsonReader.beginObject();
    while (jsonReader.hasNext()) {
      String key = jsonReader.nextName();
      switch (key) {
        case "total":
          jsonReader.skipValue(); // This isn't useful yet
          break;
        case "message":
          printMessage(jsonReader.nextString());
          break;
        case "results":
          jsonReader.beginArray();
          while (jsonReader.hasNext()) {
            jsonReader.beginArray();

            HashMap<String, String> suggestResult = new HashMap<>();
            suggestResult.put("value", jsonReader.nextString());
            suggestResult.put("label", jsonReader.nextString());
            valuesToSuggest.add(suggestResult);

            jsonReader.endArray();
          }
          jsonReader.endArray();
          break;
        default:
          // Just ignore anything else
          jsonReader.skipValue();
          break;
      }
    }
    jsonReader.endObject();
  }
  
}
