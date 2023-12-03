package io.sandboxmc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.HashMap;

import com.google.gson.internal.JavaVersion;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.ServerCommandSource;

public class Web {
  public static final String WEB_DOMAIN = "https://www.sandboxmc.dev";
  public static final String MOD_VERSION = FabricLoader.getInstance().getModContainer(Main.modId).get().toString().replace("sandboxmc ", "");

  private static HashMap<String, String> bearerTokens = new HashMap<String, String>();

  public static String getBearerToken(String key) {
    // TODO: handle inactivity timeouts
    return bearerTokens.get(key);
  }

  public static void setBearerToken(String key, String value) {
    bearerTokens.put(key, value);
  }

  private ServerCommandSource source;
  private Builder requestBuilder;

  public Web(ServerCommandSource commandSource) {
    source = commandSource;
    requestBuilder = HttpRequest.newBuilder()
      // Always want to specify our user agent
      .header("User-Agent", userAgent())
      // NORMALLY we're interacting with a JSON endpoint, let's just default to that.
      .header("Content-Type", "application/json");
  }

  public Web(ServerCommandSource commandSource, String path) {
    this(commandSource);

    setPath(path);
  }

  public Web(ServerCommandSource commandSource, String path, Boolean withAuth) {
    this(commandSource, path);

    if (withAuth) {
      setAuth(getBearerToken(source.getPlayer().getUuidAsString()));
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
    requestBuilder = requestBuilder.POST(BodyPublishers.ofString(json));
  }

  public void setPostBody(File file) throws FileNotFoundException {
    requestBuilder = requestBuilder.header("Content-Type", "multipart/form-data");
    requestBuilder = requestBuilder.POST(BodyPublishers.ofFile(file.toPath()));
  }

  public void setPutBody(String json) {
    requestBuilder = requestBuilder.PUT(BodyPublishers.ofString(json));
  }

  public void setAuth(String authToken) {
    requestBuilder = requestBuilder.header("Authorization", "Bearer " + authToken);
  }

  public InputStream getInputStream() throws IOException, InterruptedException {
    return HttpClient.newHttpClient().send(requestBuilder.build(), BodyHandlers.ofInputStream()).body();
  }

  public String getString() throws IOException, InterruptedException {
    return HttpClient.newHttpClient().send(requestBuilder.build(), BodyHandlers.ofString()).body();
  }

  public String userAgent() {
    return "SandboxMC Agent (" + MOD_VERSION + "); Minecraft (" + source.getServer().getVersion() + "); Java (" + JavaVersion.getMajorJavaVersion() + ");";
  }
}
