package io.sandboxmc.commands;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;

import com.google.gson.stream.JsonReader;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.sandboxmc.Web;
import io.sandboxmc.commands.arguments.AuthCodeArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class WebAuthenticate {
  private static HashMap<String, String> authTokens = new HashMap<String, String>();

  public static LiteralArgumentBuilder<ServerCommandSource> register() {
    return CommandManager.literal("authenticate")
      .then(
        CommandManager.argument("auth-code", AuthCodeArgumentType.authCode())
        .executes(context -> submitAuthCode(context))
      )
      .executes(context -> getAuthToken(context));
  }

  private static int getAuthToken(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    Web web = new Web(context.getSource(), "/clients/auth/init");
    // TODO: figure out a "test env" situation for this
    web.setPostBody("{\"auth\": {\"uuid\": \"ea5a400f-678c-4fcb-853b-3d948476a0c6\"}}");
    // web.setPostBody("{\"auth\": {\"uuid\": \"" + context.getSource().getPlayer().getUuidAsString() + "\"}}");

    try {
      JsonReader jsonReader = new JsonReader(new StringReader(web.getString()));
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

      // Store this for the subsequent post in `submitAuthCode`, it can then be dropped after that.
      authTokens.put(context.getSource().getPlayer().getUuidAsString(), authToken);

      MutableText authText = Text.literal("Please visit the following link to continue authentication\n");
      String authUrl = Web.WEB_DOMAIN + "/clients/auth/login/" + authToken;
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
    String authToken = authTokens.get(context.getSource().getPlayer().getUuidAsString());
    if (authToken == null) {
      printHelpMessage(context);
      return 0;
    }
    
    String authCode = AuthCodeArgumentType.getAuthCode(context, "auth-code");

    Web web = new Web(context.getSource(), "/clients/auth/verify", authToken);
    web.setPutBody("{\"auth\": {\"code\": \"" + authCode + "\"}}");
    
    try {
      JsonReader jsonReader = new JsonReader(new StringReader(web.getString()));

      jsonReader.beginObject();
      while (jsonReader.hasNext()) {
        String key = jsonReader.nextName();
        switch (key) {
          case "bearer_token":
            String playerUUID = context.getSource().getPlayer().getUuidAsString();
            Web.setBearerToken(playerUUID, jsonReader.nextString());

            // We're now effectively logged in, we can drop the auth token
            // now we'll use the bearer token for the player's UUID to make any auth-required calls for this session.
            authTokens.remove(playerUUID);
            break;
          default:
            // Just ignore anything else
            jsonReader.skipValue();
            break;
        }
      }
      jsonReader.endObject();
      jsonReader.close();
      
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
    MutableText helpURL = Text.literal("[ " + Web.WEB_DOMAIN + "/clients/auth/help ]");
    ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.OPEN_URL, Web.WEB_DOMAIN + "/clients/auth/help");
    helpURL.setStyle(Style.EMPTY.withClickEvent(clickEvent));
    helpURL.formatted(Formatting.UNDERLINE).formatted(Formatting.AQUA);
    helpText.append(helpURL);
    helpText.append(Text.literal(" for more instructions on how to authenticate your client."));
    context.getSource().sendFeedback(() -> {
      return helpText;
    }, false);
  }
}
