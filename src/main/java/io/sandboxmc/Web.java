package io.sandboxmc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.HashMap;

import com.google.gson.internal.JavaVersion;
import com.google.gson.stream.JsonReader;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;

public class Web {
  public static final String WEB_DOMAIN = "https://www.sandboxmc.dev";
  public static final String MOD_VERSION = FabricLoader.getInstance().getModContainer(Main.modId).get().toString().replace("sandboxmc ", "");

  //==============================================================
  //
  // Static definition of Web.
  // Purely used for storing session information (bearer tokens).
  // TODO: need to write a loader so we can boot sessions up on server load when configed.
  //
  //==============================================================
  private static HashMap<String, String> bearerTokens = new HashMap<String, String>();

  public static String getBearerToken(String key) {
    // TODO: handle inactivity timeouts
    return bearerTokens.get(key);
  }

  public static String getBearerToken(ServerPlayerEntity player) {
    return getBearerToken(player.getUuidAsString());
  }

  public static String getBearerToken(ServerCommandSource source) {
    return getBearerToken(source.getPlayer());
  }

  public static void setBearerToken(String key, String token) {
    bearerTokens.put(key, token);
  }

  public static void removeBearerToken(String key) {
    bearerTokens.remove(key);
  }

  public static void removeBearerToken(ServerPlayerEntity player) {
    removeBearerToken(player.getUuidAsString());
  }

  public static void removeBearerToken(ServerCommandSource source) {
    removeBearerToken(source.getPlayer());
  }

  //==============================================================
  //
  // Instance definition of Web.
  // Used for building API requests.
  //
  //==============================================================
  private ServerCommandSource source;
  private Builder requestBuilder;
  private Boolean hasAuth = false;
  // Readers for fetching data.
  private JsonReader jsonReader = null;
  private StringReader stringReader = null;
  private InputStream inputStream = null;

  public Web(ServerCommandSource commandSource) {
    source = commandSource;
    requestBuilder = HttpRequest.newBuilder()
      .header("User-Agent", userAgent())
      .header("Accept", "*/*");
  }

  public Web(ServerCommandSource commandSource, String path) {
    this(commandSource);

    setPath(path);
  }

  public Web(ServerCommandSource commandSource, String path, Boolean withAuth) {
    this(commandSource, path);

    if (withAuth) {
      setAuth(getBearerToken(commandSource));
    }
  }

  public Web(ServerCommandSource commandSource, String path, String authToken) {
    this(commandSource, path);

    setAuth(authToken);
  }

  public void setPath(String path) {
    requestBuilder = requestBuilder.uri(URI.create(WEB_DOMAIN + path));
  }

  public void setPostBody(String json) {
    requestBuilder = requestBuilder.setHeader("Content-Type", "application/json");
    requestBuilder = requestBuilder.POST(BodyPublishers.ofString(json));
  }

  public void setPostBody(File file) throws FileNotFoundException {
    requestBuilder = requestBuilder.setHeader("Content-Type", "multipart/form-data");
    requestBuilder = requestBuilder.POST(BodyPublishers.ofFile(file.toPath()));
  }

  public void setPutBody(String json) {
    requestBuilder = requestBuilder.setHeader("Content-Type", "application/json");
    requestBuilder = requestBuilder.PUT(BodyPublishers.ofString(json));
  }

  public void setDeleteBody() {
    requestBuilder = requestBuilder.DELETE();
  }

  public void setAuth(String authToken) {
    if (authToken != null && authToken.length() > 0) {
      hasAuth = true;
      requestBuilder = requestBuilder.setHeader("Authorization", "Bearer " + authToken);
    }
  }

  public Boolean hasAuth() {
    return hasAuth;
  }

  public String getString() throws IOException, InterruptedException {
    return HttpClient.newHttpClient().send(requestBuilder.build(), BodyHandlers.ofString()).body();
  }

  public InputStream getInputStream() throws IOException, InterruptedException {
    inputStream = HttpClient.newHttpClient().send(requestBuilder.build(), BodyHandlers.ofInputStream()).body();
    return inputStream;
  }

  public JsonReader getJson() throws IOException, InterruptedException {
    stringReader = new StringReader(getString());
    jsonReader = new JsonReader(stringReader);
    return jsonReader;
  }

  public void closeReaders() {
    if (inputStream != null) {
      try {
        inputStream.close();
      } catch (IOException e) {
        // do nothing, maybe it was just already closed?
      }
    }

    if (jsonReader != null) {
      try {
        jsonReader.close();
      } catch (IOException e) {
        // do nothing, maybe it was just already closed?
      }
    }

    // Ensure this happens AFTER json reader, since the json reader is what actually uses it.
    // We do want to make sure we close it though.
    if (stringReader != null) {
      stringReader.close();
    }
  }

  public String userAgent() {
    return "SandboxMC Agent (" + MOD_VERSION + "); Minecraft (" + source.getServer().getVersion() + "); Java (" + JavaVersion.getMajorJavaVersion() + ");";
  }
}
