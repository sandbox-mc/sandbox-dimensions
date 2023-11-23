package io.sandbox.dimensions.commands;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

import org.spongepowered.asm.util.JavaVersion;

import com.google.gson.stream.JsonReader;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.sandbox.dimensions.Main;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class WebAuthenticate {
  public static String userAgent(CommandContext<ServerCommandSource> context) {
    return "SandboxMC Agent (MODVERSION); Java (" + JavaVersion.current() + "); Minecraft (" + context.getSource().getServer().getVersion() + ")";
  }

  public static LiteralArgumentBuilder<ServerCommandSource> register() {
    return CommandManager.literal("authenticate")
      .then(
        // TODO: try and use the custom AuthCodeArgumentType so we can do custom validation...
        CommandManager.argument("auth-code", StringArgumentType.string())
        .executes(context -> submitAuthCode(context))
      )
      .executes(context -> getAuthToken(context));
  }

  private static int getAuthToken(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    HttpRequest request = HttpRequest.newBuilder()
      .uri(URI.create(Main.WEB_DOMAIN + "/clients/auth/init"))
      .header("User-Agent", userAgent(context))
      .header("Content-Type", "application/json")
      .POST(HttpRequest.BodyPublishers.ofString("{\"auth\": {\"uuid\": \"ea5a400f-678c-4fcb-853b-3d948476a0c6\"}}"))
      // .POST(HttpRequest.BodyPublishers.ofString("{\"uuid\": \"" + context.getSource().getPlayer().getUuidAsString() + "\"}"))
      .build();

    try {
      HttpClient client = HttpClient.newHttpClient();
      HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
      JsonReader jsonReader = new JsonReader(new StringReader(response.body()));
      String authToken = null;

      jsonReader.beginObject();
      while (jsonReader.hasNext()) {
        String key = jsonReader.nextName();
        switch (key) {
          case "auth_token":
            authToken = jsonReader.nextString();
            break;
          default:
            // Just ignore anything else
            jsonReader.skipValue();
            break;
        }
      }
      jsonReader.endObject();
      jsonReader.close();

      if (authToken == null) {
        return 0;
      }

      MutableText authText = Text.literal("Please visit the following link to continue authentication\n");
      String authUrl = Main.WEB_DOMAIN + "/clients/auth/login/" + authToken;
      MutableText clickableUrl = Text.literal("[ " + authUrl + " ]");
      ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.OPEN_URL, authUrl);
      clickableUrl.setStyle(Style.EMPTY.withClickEvent(clickEvent));
      clickableUrl.formatted(Formatting.UNDERLINE).formatted(Formatting.DARK_AQUA);
      authText.append(clickableUrl);
      context.getSource().sendFeedback(() -> {
        return authText;
      }, false);
    } catch (InterruptedException e) {
      printHelpMessage(context);
      return 0;
    } catch (IOException e) {
      printHelpMessage(context);
      return 0;
    }

    return 1;
  }

  private static int submitAuthCode(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    String authCode = StringArgumentType.getString(context, "auth-code");
    String authToken = "TODO";

    HttpRequest request = HttpRequest.newBuilder()
      .uri(URI.create(Main.WEB_DOMAIN + "/clients/auth/verify"))
      .header("User-Agent", userAgent(context))
      .header("Content-Type", "application/json")
      .PUT(HttpRequest.BodyPublishers.ofString("{\"auth\": {\"token\": \"" + authToken + "\", \"code\": \"" + authCode + "\"}}"))
      .build();
    
    try {
      HttpClient client = HttpClient.newHttpClient();
      HttpResponse<String> response = client.send(request, BodyHandlers.ofString());
      System.out.println("RESPONSE BODY: " + response.body());
      // JsonReader jsonReader = new JsonReader(new StringReader(response.body()));
      
    } catch (InterruptedException e) {
      printHelpMessage(context);
      return 0;
    } catch (IOException e) {
      printHelpMessage(context);
      return 0;
    }

    return 1;
  }

  private static void printHelpMessage(CommandContext<ServerCommandSource> context) {
    MutableText helpText = Text.literal("Something went wrong. Please visit ");
    MutableText helpURL = Text.literal("[ " + Main.WEB_DOMAIN + "/clients/auth/help ]");
    ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.OPEN_URL, Main.WEB_DOMAIN + "/clients/auth/help");
    helpURL.setStyle(Style.EMPTY.withClickEvent(clickEvent));
    helpURL.formatted(Formatting.UNDERLINE).formatted(Formatting.AQUA);
    helpText.append(helpURL);
    helpText.append(Text.literal(" for more instructions on how to authenticate your client."));
    context.getSource().sendFeedback(() -> {
      return helpText;
    }, false);
  }
}
