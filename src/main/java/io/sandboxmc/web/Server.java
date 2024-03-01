package io.sandboxmc.web;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.UUID;

import com.google.gson.stream.JsonReader;
import com.mojang.authlib.GameProfile;
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
  private static GameProfile serverOwner = null;

  public static void authOnBoot(MinecraftServer server) {
    // Keep this threaded for faster boot times
    new Server(server).runTask("readFilesOnBoot");
  }

  public static void handleAuthForShutdown(MinecraftServer server) {
    // This is intentionally not threaded just in case there are race conditions
    new Server(server).writeAuthTokenToFile();
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
  private String task = null;

  // Intended for use with commands.
  public Server(CommandContext<ServerCommandSource> theContext) {
    super(theContext);

    server = source.getServer();
  }

  // Intended for use with server booting.
  public Server(MinecraftServer theServer) {
    server = theServer;
  }

  public void runTask(String theTask) {
    task = theTask;
    new Thread(this).start();
  }

  public void printInfo() {
    MutableText text = Text.literal("Server Info:");
    text.append("\nUUID: " + (uuid == null ? "NOT YET ASSIGNED" : uuid));
    text.append("\nServer Owner: " + (serverOwner == null ? (authToken == null ? "SERVER NOT AUTHENTICATED" : "NOT YET CLAIMED") : serverOwner.getName()));
    printMessage(text);
  }

  public void run() {
    // This is server-only and doesn't require a user.
    if (task == "readFilesOnBoot") {
      readFilesOnBoot();
      return;
    }

    if (!Web.hasBearerToken(source.getPlayer())) {
      printMessage("Please log in first.");
      return;
    }

    if (uuid == null) {
      printMessage("The server has not been registered with https://www.sandboxmc.com\nPlease ensure it has a connection to the internet and restart it.");
      return;
    }

    switch (task) {
      case "recover":
        recoverServer(false);
        return;
      case "forceRecover":
        recoverServer(true);
        return;
    }

    if (authToken == null) {
      printMessage("The server is not currently authenticated.\nIf you are the owner you can use the RECOVER command.");
      return;
    }
    
    // When adding new commands here make sure to update the VALID_COMMANDS list in ServerCmdsAutoComplete
    switch (task) {
      case "claim":
        claimServer();
        break;
      case "unclaim":
        unclaimServer();
        break;
      default:
        printMessage("Invalid server command `" + task + "`.");
        break;
    }
  }

  private void claimServer() {
    Web web = new Web(source, "/mc/server/auth/" + uuid + "/claim", true);
    web.setPatchBody(new ServerIdentifier(server).getJSON(authToken));

    try {
      JsonReader jsonReader = web.getJson();
      jsonReader.beginObject();
      while (jsonReader.hasNext()) {
        String key = jsonReader.nextName();
        switch (key) {
          case "message":
            printMessage(jsonReader.nextString());
            break;
          default:
            // Just ignore anything else for now
            jsonReader.skipValue();
            break;
        }
      }
      jsonReader.endObject();

      if (web.getStatusCode() == 200) {
        serverOwner = source.getPlayer().getGameProfile();
      }
    } catch (IOException e) {
      // What might be happening here...? Failed connections?
    } finally {
      web.closeReaders();
    }
  }

  private void unclaimServer() {
    Web web = new Web(source, "/mc/server/auth/" + uuid + "/unclaim", true);
    web.setDeleteBody(new ServerIdentifier(server).getJSON(authToken));

    try {
      JsonReader jsonReader = web.getJson();
      jsonReader.beginObject();
      while (jsonReader.hasNext()) {
        String key = jsonReader.nextName();
        switch (key) {
          case "message":
            printMessage(jsonReader.nextString());
            break;
          default:
            // Just ignore anything else for now
            jsonReader.skipValue();
            break;
        }
      }
      jsonReader.endObject();

      if (web.getStatusCode() == 200) {
        serverOwner = null;
      }
    } catch (IOException e) {
      // What might be happening here...? Failed connections?
    } finally {
      web.closeReaders();
    }
  }

  private void readFilesOnBoot() {
    File uuidFile = server.getFile(UUID_FILE_NAME);
    File authTokenFile = server.getFile(AUTH_TOKEN_FILE_NAME);

    if (uuidFile.exists()) {
      try {
        setUUID(Files.readString(uuidFile.toPath()).trim());
        System.out.println("SandboxMC UUID assigned from file: " + uuid);

        if (authTokenFile.exists()) {
          setAuthToken(Files.readString(authTokenFile.toPath()).trim());
          // Then throw out the file, this ideally exists only through restarts
          authTokenFile.delete();

          // Now we need to reauth so we know our token is fresh for security!
          reauthFromWeb();
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
    web.setPostBody(new ServerIdentifier(server).getJSON(null));
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
      jsonReader.endObject();

      System.out.println("SandboxMC UUID from web: " + uuid);
    } catch (IOException e) {
      // What might be happening here...? Failed connections?
      // Let's just make sure we're clearing things...
      setUUID(null);
      setAuthToken(null);
    } finally {
      web.closeReaders();
    }
  }

  private void recoverServer(Boolean forceReauth) {
    if (!forceReauth && authToken != null) {
      printMessage("The server appears to already be authenticated.\nIf you believe you are seeing this in error you can try forceRecovery instead.");
      return;
    }
    
    Web web = new Web(source, "/mc/server/auth/" + uuid + "/recover", true);
    web.setPatchBody(new ServerIdentifier(server).getJSON(authToken));

    try {
      readServerAuthResponse(web);
    } catch (IOException e) {
      // What might be happening here...? Failed connections? Bad auth tokens?
      // Let's just make sure we're clearing the auth token at least
      setAuthToken(null);
      serverOwner = null;
    } finally {
      web.closeReaders();
    }
  }

  private void reauthFromWeb() {
    Web web = new Web(server);
    web.setPath("/mc/server/auth/" + uuid + "/reauth");
    web.setPatchBody(new ServerIdentifier(server).getJSON(authToken));
    try {
      readServerAuthResponse(web);
    } catch (IOException e) {
      // What might be happening here...? Failed connections? Bad auth tokens?
      // Let's just make sure we're clearing the auth token at least
      setAuthToken(null);
      System.out.println("\n\nFAILED TO REAUTH ON BOOT\n\n");
    } finally {
      web.closeReaders();
    }
  }

  private void readServerAuthResponse(Web web) throws IOException {
    JsonReader jsonReader = web.getJson();
    jsonReader.beginObject();
    while (jsonReader.hasNext()) {
      String key = jsonReader.nextName();
      switch (key) {
        case "message":
          printMessage(jsonReader.nextString());
          break;
        case "auth_token":
          setAuthToken(jsonReader.nextString());
          System.out.println("SandboxMC server auth token set from web.");
          break;
        case "owner":
          jsonReader.beginObject();
          UUID ownerUUID = null;
          String ownerName = null;
          while (jsonReader.hasNext()) {
            key = jsonReader.nextName();
            switch (key) {
              case "uuid":
                try {
                  // UUIDs on MinecraftProfile on the site are stored as trimmed
                  ownerUUID = PlayerIdentifier.uuidFromTrimmed(jsonReader.nextString());
                } catch (IllegalStateException e) {
                  // Ignore this, just means the value was null
                  jsonReader.skipValue();
                } catch (IllegalArgumentException e) {
                  // UUID was a string but was formatted improperly
                }
                break;
              case "name":
                try {
                  ownerName = jsonReader.nextString();
                } catch (IllegalStateException e) {
                  // Ignore this, just means the value was null
                  jsonReader.skipValue();
                }
                break;
              default:
                jsonReader.skipValue();
                break;
            }
          }
          jsonReader.endObject();

          if (ownerUUID != null && ownerName != null) {
            serverOwner = new GameProfile(ownerUUID, ownerName);
          }
          break;
        default:
          // Just ignore anything else for now
          jsonReader.skipValue();
          break;
      }
    }
    jsonReader.endObject();
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
