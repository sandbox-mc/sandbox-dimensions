package io.sandboxmc;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;
import java.net.http.HttpResponse.BodyHandlers;

import io.sandboxmc.web.AuthManager;
import io.sandboxmc.web.InfoManager;
import net.minecraft.server.command.ServerCommandSource;

public class Web {
  public static final String WEB_DOMAIN = "https://www.sandboxmc.dev";

  private ServerCommandSource source;
  private String url;
  private Builder requestBuilder;

  public Web(ServerCommandSource commandSource) {
    source = commandSource;
    // We always want to specify our user agent and the API is always JSON.
    requestBuilder = HttpRequest.newBuilder()
      .header("User-Agent", InfoManager.userAgent(source.getServer()))
      .header("Content-Type", "application/json");
  }

  public Web(ServerCommandSource commandSource, String path) {
    this(commandSource);

    setPath(path);
  }

  public Web(ServerCommandSource commandSource, String path, Boolean withAuth) {
    this(commandSource, path);

    if (withAuth) {
      setAuth(AuthManager.getBearerToken(source.getPlayer().getUuidAsString()));
    }
  }

  public Web(ServerCommandSource commandSource, String path, String authToken) {
    this(commandSource, path);

    setAuth(authToken);
  }

  public void setPath(String path) {
    url = WEB_DOMAIN + path;
  }

  public String getPath() {
    return url;
  }

  public void setPostBody(String json) {
    requestBuilder = requestBuilder.POST(HttpRequest.BodyPublishers.ofString(json));
  }

  public void setPutBody(String json) {
    requestBuilder = requestBuilder.PUT(HttpRequest.BodyPublishers.ofString(json));
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
}
