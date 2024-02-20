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
  private static final String UUID_FILE_NAME = "sandboxmc.server.uuid";
  private static final String AUTH_TOKEN_FILE_NAME = "sandboxmc.server.token";
  private static String uuid = null;
  private static String authToken = null;

  public static void authOnBoot(MinecraftServer server) {
    // Keep this threaded for faster boot times
    Runnable thread = new Server(server);
    new Thread(thread).start();
  }

  public static void handleAuthForShutdown(MinecraftServer server) {
    // I don't think this can be threaded...
    new Server(server).writeAuthTokenToFile();
  }

  public static Boolean needsUUID() {
    return uuid == null;
  }

  public static Boolean needsAuthToken() {
    return authToken == null;
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
  private Boolean isServerEvent = false;

  // Intended for use with commands.
  public Server(CommandContext<ServerCommandSource> theContext) {
    super(theContext);

    server = source.getServer();
  }

  // Intended for use with server booting.
  public Server(MinecraftServer theServer) {
    server = theServer;
    isServerEvent = true;
  }

  public void printInfo() {
    MutableText text = Text.literal("Server info:\n");
    if (needsUUID()) {
      text.append("NO UUID!");
    }
    printMessage(text);
  }

  public void run() {
    if (isServerEvent) {
      readFilesOnBoot();
      return;
    }
  }

  private void readFilesOnBoot() {
    File uuidFile = server.getFile(UUID_FILE_NAME);
    File authTokenFile = server.getFile(AUTH_TOKEN_FILE_NAME);

    if (uuidFile.exists()) {
      try {
        setUUID(Files.readString(uuidFile.toPath()));

        if (authTokenFile.exists()) {
          setAuthToken(Files.readString(authTokenFile.toPath()));
          // Then throw out the file, this ideally exists only through restarts
          authTokenFile.delete();
        }
      } catch (IOException e) {
        // This should NOT break...
        System.out.println(e.getMessage());
      }
    } else {
      // If no uuid file was even found then we just need to request a new UUID and auth token!
      assignUUIDFromWeb();
    }
  }

  private void assignUUIDFromWeb() {
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
            writeUUIDToFile(); // It's OK to permanently store this
            break;
          case "auth_token":
            // Tfhis is only ever stored to a file on server shutdown
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
      System.out.println("\n\nAUTH HAPPENED ON BOOT?\nuuid: " + uuid + " - authToken: " + authToken + "\n\n");
    }
  }

  private void writeUUIDToFile() {
    if (uuid == null) {
      // Don't delete any possibly existing files, it could be that we failed to load the file previously
      return;
    }

    try {
      File uuidFile = server.getFile(UUID_FILE_NAME);
      if (uuidFile.exists()) {
        // Make sure we're not appending to an existing file...
        uuidFile.delete();
      }

      // Now let's write out to the file.
      BufferedWriter writer = new BufferedWriter(new FileWriter(uuidFile));
      writer.write(uuid);
      writer.close();
    } catch (IOException e) {
      // This should NOT happen, if it does we need to know why!
      System.out.println(e.getMessage());
    }
  }

  private void writeAuthTokenToFile() {
    if (authToken == null) {
      // Don't delete any possibly existing files, it could be that we failed to load the file previously
      return;
    }

    try {
      File authTokenFile = server.getFile(AUTH_TOKEN_FILE_NAME);
      if (authTokenFile.exists()) {
        // Make sure we're not appending to an existing file...
        authTokenFile.delete();
      }

      // Now let's write out to the file.
      BufferedWriter writer = new BufferedWriter(new FileWriter(authTokenFile));
      writer.write(authToken);
      writer.close();
    } catch (IOException e) {
      // This should NOT happen, if it does we need to know why!
      System.out.println(e.getMessage());
    }
  }
}
