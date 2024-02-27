package io.sandboxmc.commands.autoComplete.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import com.google.gson.stream.JsonReader;

import io.sandboxmc.Web;
import net.minecraft.server.command.ServerCommandSource;

public class WebAutoCompleteWorker implements Runnable {
  private static HashMap<String, ArrayList<HashMap<String, String>>> cache = new HashMap<>();
  // private static ArrayList<String> playersFetching = new ArrayList<String>();

  private ServerCommandSource source;
  private String cacheKey;
  private String pathToFetch;
  private Boolean restrictToAuth = false;

  public WebAutoCompleteWorker(ServerCommandSource theSource, String thePath, Boolean doRestrictToAuth) {
    source = theSource;
    String playerUUID = source.getPlayer().getUuidAsString();
    pathToFetch = thePath;
    restrictToAuth = doRestrictToAuth;
    // TODO: can I set this with a like... several minute cache?
    cacheKey = playerUUID + "|||" + restrictToAuth + "|||" + thePath;
  }

  public ArrayList<HashMap<String, String>> getFromCache() {
    // TODO: also verify that the current user can only have one request outgoing at a time...
    if (cache.containsKey(cacheKey)) {
      return cache.get(cacheKey);
    } else {
      return null;
    }
  }

  public void run() {
    Web web = new Web(source, pathToFetch, restrictToAuth);

    if (restrictToAuth && !web.hasAuth()) {
      return;
    }

    ArrayList<HashMap<String, String>> valuesToSuggest = new ArrayList<>();
    try {
      readJSON(web.getJson(), valuesToSuggest);
      cache.put(cacheKey, valuesToSuggest);
    } catch (IOException e) {
      System.out.println("Error: " + e.getClass().toString() + " - " + e.getMessage());
    } finally {
      web.closeReaders();
    }
  }

  // @see https://stackoverflow.com/questions/4308554/simplest-way-to-read-json-from-a-url-in-java
  private void readJSON(JsonReader jsonReader, ArrayList<HashMap<String, String>> valuesToSuggest) throws IOException {
    jsonReader.beginObject();
    while (jsonReader.hasNext()) {
      String key = jsonReader.nextName();
      switch (key) {
        case "total":
          jsonReader.skipValue(); // TODO: determine if the total count can be used
          break;
        case "message":
          jsonReader.skipValue(); // TODO: determine if error messages can be used
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
