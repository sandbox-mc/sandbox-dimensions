package io.sandboxmc.web;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

import com.google.gson.stream.JsonReader;
import com.mojang.brigadier.context.CommandContext;

import io.sandboxmc.Web;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

public class Server extends Common implements Runnable {
  // Class level methods for interacting with the server's auth.
  private static final String UUID_FILE_NAME = "sbmc.server.uuid";
  private static String uuid = null;
  private static String authToken = null;

  public static void authOnBoot(MinecraftServer server) {
    Runnable thread = new Server(server);
    new Thread(thread).start();
  }

  public static Boolean needsUUID() {
    return uuid == null;
  }

  public static String getUUID() {
    return uuid;
  }

  public static void setUUID(String newUUID) {
    uuid = newUUID;
  }

  public static String getAuthToken() {
    return authToken;
  }

  public static void setAuthToken(String newAuthToken) {
    authToken = newAuthToken;
  }

  // Instance methods for authing server. This all runs as a thread.
  private MinecraftServer server;
  private Boolean isBoot = false;

  // Intended for use with commands.
  public Server(CommandContext<ServerCommandSource> theContext) {
    super(theContext);

    server = source.getServer();
  }

  // Intended for use with server booting.
  public Server(MinecraftServer theServer) {
    server = theServer;
    isBoot = true;
  }

  public void printInfo() {
    MutableText text = Text.literal("Server info:\n");
    if (needsUUID()) {
      text.append("NO UUID!");
    }
    printMessage(text);
  }

  public void run() {
    checkUUID(); // This may possibly call one or more synchronous web calls!
  }

  private void checkUUID() {
    if (!needsUUID()) {
      return;
    }

    // attempt to read from file first.
    File uuidFile = server.getFile(UUID_FILE_NAME);

    if (uuidFile.exists()) {
      try {
        setUUID(Files.readString(uuidFile.toPath()));
        // TODO: we also need to set the authToken in this case...
        // If there IS no auth token then we'll need to have a recovery path of some sort
        // or else just say that they have to assign a new UUID...
      } catch (IOException e) {
        // This should NOT break...
        System.out.println(e.getMessage());
      }
    } else {
      assignUUID();
    }
  }

  private void assignUUID() {
    Web web = new Web(server);
    web.setPath("/mc/server/auth/assign-uuid");
    web.setPostBody(new ServerIdentifier(server).getJSON());
    try {
      JsonReader jsonReader = web.getJson();
      jsonReader.beginObject();
      while (jsonReader.hasNext()) {
        String key = jsonReader.nextName();
        switch (key) {
          case "uuid":
            setUUID(jsonReader.nextString());
            writeUUIDToFile(getUUID()); // It's OK to permanently store this
            break;
          case "auth_token":
            // This is only ever stored to a file on server shutdown.
            // Also only then if configured to do so!
            setAuthToken(jsonReader.nextString());
            break;
          default:
            // Just ignore anything else for now
            jsonReader.skipValue();
            break;
        }
      }
    } catch (IOException e) {
      // should be able to ignore this safely
    } finally {
      web.closeReaders();
    }
  }

  private void writeUUIDToFile(String theUUID) {
    try {
      BufferedWriter writer = new BufferedWriter(new FileWriter(server.getFile(UUID_FILE_NAME)));
      writer.write(theUUID);
      writer.close();
    } catch (IOException e) {
      // This should NOT happen, if it does we need to know why!
      System.out.println(e.getMessage());
    }
  }
}
