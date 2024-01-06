package io.sandboxmc.commands;

import java.io.IOException;
import java.util.HashMap;

import com.google.gson.stream.JsonReader;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.sandboxmc.Web;
import io.sandboxmc.web.PlayerIdentifier;
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

  // Very basic wrapper around the thread.
  // This is a blind function and does not know the outcome of the thread at the time of command completion.
  private static int getAuthToken(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    Runnable webAuthThread = new WebAuthenticate(context, "getAuthToken");
    new Thread(webAuthThread).start();

    return 1;
  }

  // Very basic wrapper around the thread.
  // This is a blind function and does not know the outcome of the thread at the time of command completion.
  private static int submitAuthCode(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    Runnable webAuthThread = new WebAuthenticate(context, "submitAuthCode");
    new Thread(webAuthThread).start();

    return 1;
  }

  private CommandContext<ServerCommandSource> context;
  private ServerCommandSource source;
  private PlayerIdentifier playerID;
  private String authStage;

  public WebAuthenticate(CommandContext<ServerCommandSource> theContext, String stage) {
    context = theContext;
    source = context.getSource();
    playerID = new PlayerIdentifier(source.getPlayer());
    authStage = stage;
  }

  public void run() {
    if (Web.getBearerToken(source) != null) {
      printMessage("Already authenticated.");
      return;
    }

    switch (authStage) {
      case "getAuthToken":
        getAuthTokenThread();
        break;
      case "submitAuthCode":
        submitAuthCodeThread();
        break;
    }
  }

  private void getAuthTokenThread() {
    Web web = new Web(source, "/clients/auth/init");
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

    Web web = new Web(source, "/clients/auth/verify", authToken);
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
