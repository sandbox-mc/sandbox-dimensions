package io.sandboxmc.commands;

import java.io.IOException;
import java.util.HashMap;

import com.google.gson.stream.JsonReader;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.sandboxmc.Web;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class WebAuthenticate implements Runnable {
  private static final String AUTH_CODE_REGEX = "^\\d{6}$";
  private static HashMap<String, String> authTokens = new HashMap<String, String>();

  public static LiteralArgumentBuilder<ServerCommandSource> register() {
    return CommandManager.literal("authenticate")
      .then(
        CommandManager.argument("auth-code", StringArgumentType.word())
        .executes(context -> submitAuthCode(context))
      )
      .executes(context -> getAuthToken(context));
  }

  private static int getAuthToken(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    Runnable webAuthThread = new WebAuthenticate(context, "getAuthToken");
    new Thread(webAuthThread).start();

    return 1;
  }

  private static int submitAuthCode(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    Runnable webAuthThread = new WebAuthenticate(context, "submitAuthCode");
    new Thread(webAuthThread).start();

    return 1;
  }

  private CommandContext<ServerCommandSource> context;
  private String mode;

  public WebAuthenticate(CommandContext<ServerCommandSource> theContext, String theMode) {
    context = theContext;
    mode = theMode;
  }

  public void run() {
    switch (mode) {
      case "getAuthToken":
        getAuthTokenThread();
        break;
      case "submitAuthCode":
        submitAuthCodeThread();
        break;
    }
  }

  private void getAuthTokenThread() {
    Web web = new Web(context.getSource(), "/clients/auth/init");
    String playerUUID = context.getSource().getPlayer().getUuidAsString();
    // Uncomment if using the test client that boots with a made up user.
    // playerUUID = "ea5a400f-678c-4fcb-853b-3d948476a0c6"; // Cardtable
    web.setPostBody("{\"auth\": {\"uuid\": \"" + playerUUID + "\"}}");

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

      // Store this for the subsequent post in `submitAuthCode`, it can then be dropped after that.
      authTokens.put(context.getSource().getPlayer().getUuidAsString(), authToken);

      MutableText authText = Text.literal("Please visit the following link to continue authentication\n");
      String authUrl = Web.WEB_DOMAIN + "/clients/auth/login/" + authToken;
      MutableText clickableUrl = Text.literal("[ " + authUrl + " ]");
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
    String authToken = authTokens.get(context.getSource().getPlayer().getUuidAsString());
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

    Web web = new Web(context.getSource(), "/clients/auth/verify", authToken);
    web.setPatchBody("{\"auth\": {\"code\": \"" + authCode + "\"}}");
    
    try {
      JsonReader jsonReader = web.getJson();

      jsonReader.beginObject();
      while (jsonReader.hasNext()) {
        String key = jsonReader.nextName();
        switch (key) {
          case "bearer_token":
            String playerUUID = context.getSource().getPlayer().getUuidAsString();
            Web.setBearerToken(playerUUID, jsonReader.nextString());

            // We're now effectively logged in, we can drop the auth token
            // now we'll use the bearer token for the player's UUID to make any auth-required calls for this session.
            // The server will have dropped the authCode from the record on its side and therefore we'd need to regenerate all of this anyway.
            authTokens.remove(playerUUID);
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

  private void printMessage(MutableText message) {
    context.getSource().sendFeedback(() -> {
      return message;
    }, false);
  }

  private void printMessage(String message) {
    printMessage(Text.literal(message));
  }

  private void printHelpMessage() {
    MutableText helpText = Text.literal("Something went wrong. Please visit ");
    MutableText helpURL = Text.literal("[ " + Web.WEB_DOMAIN + "/clients/auth/help ]");
    ClickEvent clickEvent = new ClickEvent(ClickEvent.Action.OPEN_URL, Web.WEB_DOMAIN + "/clients/auth/help");
    helpURL.setStyle(Style.EMPTY.withClickEvent(clickEvent));
    helpURL.formatted(Formatting.UNDERLINE).formatted(Formatting.AQUA);
    helpText.append(helpURL);
    helpText.append(Text.literal(" for more instructions on how to authenticate your client."));
    printMessage(helpText);
  }
}
