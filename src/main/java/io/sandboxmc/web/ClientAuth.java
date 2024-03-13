package io.sandboxmc.web;

import java.io.IOException;
import java.util.HashMap;

import com.google.gson.stream.JsonReader;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import io.sandboxmc.Web;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class ClientAuth extends Common implements Runnable {
  private static final String AUTH_PATH_PREFIX = "/mc/client";
  private static final String AUTH_CODE_REGEX = "^\\d{6}$";
  private static HashMap<String, String> authTokens = new HashMap<String, String>();

  private PlayerIdentifier playerID;
  private String stage;

  public ClientAuth(CommandContext<ServerCommandSource> theContext, String authStage) {
    super(theContext);

    playerID = new PlayerIdentifier(source.getPlayer());
    stage = authStage;
  }

  public void run() {
    if (Web.getBearerToken(source.getPlayer()) != null) {
      printMessage("Already authenticated.");
      return;
    }

    switch (stage) {
      case "getAuthToken":
        getAuthTokenThread();
        break;
      case "submitAuthCode":
        submitAuthCodeThread();
        break;
    }
  }

  private void getAuthTokenThread() {
    Web web = new Web(source, AUTH_PATH_PREFIX + "/auth/init");
    web.setPostBody("{\"auth\": " + playerID.getJSON() + "}");

    try {
      JsonReader jsonReader = web.getJson();
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

      if (authToken == null) {
        return;
      }

      // Store this for the subsequent post in `submitAuthCode`, it should then be dropped.
      authTokens.put(playerID.getIdentifier(), authToken);

      MutableText authText = Text.literal("Please visit the following link to continue authentication\n");
      String authUrl = Web.getWebDomain() + AUTH_PATH_PREFIX + "/auth/login/" + authToken;
      MutableText clickableUrl = Text.literal(authUrl);
      ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.OPEN_URL, authUrl);
      clickableUrl.setStyle(Style.EMPTY.withClickEvent(clickEvent));
      clickableUrl.formatted(Formatting.UNDERLINE).formatted(Formatting.DARK_AQUA);
      authText.append(clickableUrl);
      printMessage(authText);
    } catch (IOException e) {
      printHelpMessage();
      return;
    } finally {
      web.closeReaders();
    }
  }

  private void submitAuthCodeThread() {
    String authToken = authTokens.get(playerID.getIdentifier());
    if (authToken == null) {
      printMessage("Please run `/sandboxmc authenticate` first!");
      return;
    }
    
    String authCode = StringArgumentType.getString(context, "auth-code");
    if (!authCode.matches(AUTH_CODE_REGEX)) {
      printMessage("Invalid auth-code, must be exactly 6 digits. (Example: 056345)");
      return;
    }

    printMessage("Authenticating with SandboxMC...");

    Web web = new Web(source, AUTH_PATH_PREFIX + "/auth/verify", authToken);
    web.setPatchBody("{\"auth\": {\"code\": \"" + authCode + "\"}}");
    
    try {
      JsonReader jsonReader = web.getJson();

      jsonReader.beginObject();
      while (jsonReader.hasNext()) {
        String key = jsonReader.nextName();
        switch (key) {
          case "bearer_token":
            Web.setBearerToken(playerID.getIdentifier(), jsonReader.nextString());

            // We're now effectively logged in, we can drop the auth token
            // now we'll use the bearer token for the player's UUID to make any auth-required calls for this session.
            // The server will have dropped the authCode from the record on its side and therefore we'd need to regenerate all of this anyway.
            authTokens.remove(playerID.getIdentifier());
            break;
          default:
            // Just ignore anything else
            jsonReader.skipValue();
            break;
        }
      }
      jsonReader.endObject();
    } catch (IOException e) {
      printHelpMessage();
      return;
    } finally {
      // Always ensure we're closing our readers.
      web.closeReaders();
    }

    // We're now authed, tell the user.
    printMessage("Authentication successful!");
  }

  private void printHelpMessage() {
    MutableText helpText = Text.literal("Something went wrong. Please visit\n");
    String helpUrl = Web.getWebDomain() + AUTH_PATH_PREFIX + "/auth/help";
    MutableText helpURL = Text.literal(helpUrl);
    ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.OPEN_URL, helpUrl);
    helpURL.setStyle(Style.EMPTY.withClickEvent(clickEvent));
    helpURL.formatted(Formatting.UNDERLINE).formatted(Formatting.AQUA);
    helpText.append(helpURL);
    helpText.append(Text.literal("\nfor more instructions on how to authenticate your client."));
    printMessage(helpText);
  }
}
