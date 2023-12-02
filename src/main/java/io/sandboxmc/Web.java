package io.sandboxmc;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.Builder;

import com.mojang.brigadier.context.CommandContext;

import io.sandboxmc.web.AuthManager;
import io.sandboxmc.web.InfoManager;
import net.minecraft.server.command.ServerCommandSource;

public class Web {
  public static Builder initHttpRequest(CommandContext<ServerCommandSource> context, String url) {
    return HttpRequest.newBuilder()
      .uri(URI.create(url))
      .header("User-Agent", InfoManager.userAgent(context.getSource().getServer()))
      .header("Content-Type", "application/json");
  }

  public static Builder initAuthedHttpRequest(CommandContext<ServerCommandSource> context, String url) {
    return authHttpRequest(initHttpRequest(context, url), context);
  }

  public static Builder authHttpRequest(Builder requestBuilder, CommandContext<ServerCommandSource> context) {
    String authToken = AuthManager.getBearerToken(context.getSource().getPlayer().getUuidAsString());

    return requestBuilder.header("Authorization", "Bearer " + authToken);
  }
}
