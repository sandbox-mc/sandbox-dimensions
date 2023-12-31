package io.sandboxmc;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.util.HashMap;

import com.google.gson.internal.JavaVersion;
import com.google.gson.stream.JsonReader;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class Web {
  // public static final String WEB_DOMAIN = "http://127.0.0.1:3000";
  // public static final String WEB_DOMAIN = "https://www.sandboxmc.dev";
  public static final String WEB_DOMAIN = "https://www.sandboxmc.io";
  public static final String MOD_VERSION = FabricLoader.getInstance().getModContainer(Main.modId).get().toString().replace("sandboxmc ", "");

  //==============================================================
  //
  // Static definition of Web.
  // Purely used for storing session information (bearer tokens).
  // TODO: need to write a loader so we can boot sessions up on server load when configed.
  //
  //==============================================================
  private static HashMap<String, String> bearerTokens = new HashMap<String, String>();

  public static String getBearerToken(String key) {
    // TODO: handle inactivity timeouts
    return bearerTokens.get(key);
  }

  public static String getBearerToken(ServerPlayerEntity player) {
    return getBearerToken(player.getUuidAsString());
  }

  public static String getBearerToken(ServerCommandSource source) {
    return getBearerToken(source.getPlayer());
  }

  public static void setBearerToken(String key, String token) {
    bearerTokens.put(key, token);
  }

  public static void removeBearerToken(String key) {
    bearerTokens.remove(key);
  }

  public static void removeBearerToken(ServerPlayerEntity player) {
    removeBearerToken(player.getUuidAsString());
  }

  public static void removeBearerToken(ServerCommandSource source) {
    removeBearerToken(source.getPlayer());
  }

  //==============================================================
  //
  // Instance definition of Web.
  // Used for building API requests.
  //
  //==============================================================
  private ServerCommandSource source;
  private MediaType jsonMediaType = MediaType.get("application/json");
  private OkHttpClient client;
  private Request request;
  private Request.Builder requestBuilder;
  private MultipartBody.Builder formBuilder = null;
  private Response response = null;
  private ResponseBody responseBody = null;
  private Boolean hasAuth = false;
  // Readers for fetching data.
  private JsonReader jsonReader = null;
  private StringReader stringReader = null;
  private InputStream inputStream = null;
  private BufferedInputStream bufferedInputStream = null;

  public Web(ServerCommandSource commandSource) {
    source = commandSource;
    client = new OkHttpClient();
    requestBuilder = new Request.Builder()
      .header("User-Agent", userAgent())
      .header("Accept", "*/*");
  }

  public Web(ServerCommandSource commandSource, String path) {
    this(commandSource);

    setPath(path);
  }

  public Web(ServerCommandSource commandSource, String path, Boolean withAuth) {
    this(commandSource, path);

    if (withAuth) {
      setAuth(getBearerToken(commandSource));
    }
  }

  public Web(ServerCommandSource commandSource, String path, String authToken) {
    this(commandSource, path);

    setAuth(authToken);
  }

  public void setPath(String path) {
    requestBuilder = requestBuilder.url(HttpUrl.get(URI.create(WEB_DOMAIN + path)));
  }

  public void setPostBody(String json) {
    requestBuilder = requestBuilder.post(RequestBody.create(json, jsonMediaType));
  }

  // Note: This is the MultiPart version of setPostBody and requires that setFormField has been called at least once.
  public void finalizeFormAsPostBody() {
    if (formBuilder == null) {
      throw new NullPointerException("`setFormField` was never called!");
    }

    requestBuilder = requestBuilder.post(formBuilder.build());
  }

  public void setPatchBody(String json) {
    requestBuilder = requestBuilder.patch(RequestBody.create(json, jsonMediaType));
  }

  // Note: This is the MultiPart version of setPatchBody and requires that setFormField has been called at least once.
  public void finalizeFormAsPatchBody() {
    if (formBuilder == null) {
      throw new NullPointerException("`setFormField` was never called!");
    }

    requestBuilder = requestBuilder.patch(formBuilder.build());
  }

  public void setDeleteBody() {
    requestBuilder = requestBuilder.delete();
  }

  public void setFormField(String fieldName, File file) {
    if (formBuilder == null) {
      formBuilder = new MultipartBody.Builder().setType(MultipartBody.FORM);
    }
    
    formBuilder = formBuilder.addFormDataPart(fieldName, file.getName(), RequestBody.create(file, MediaType.parse("text/plain")));
  }

  public void setAuth(String authToken) {
    if (authToken != null && authToken.length() > 0) {
      hasAuth = true;
      requestBuilder = requestBuilder.addHeader("Authorization", "Bearer " + authToken);
    }
  }

  public Boolean hasAuth() {
    return hasAuth;
  }

  public void executeRequest() throws IOException {
    request = requestBuilder.build();
    response = client.newCall(request).execute();
    responseBody = response.body();
  }

  public String getString() throws IOException {
    if (responseBody == null) {
      executeRequest();
    }

    return responseBody.string();
  }

  public BufferedInputStream getInputStream() throws IOException {
    executeRequest(); // this HAS to actually execute every time for recursion.

    // TODO: handle redirect loops
    if (getStatusCode() == 301 || getStatusCode() == 302) {
      String redirectUrl = response.header("location");
      requestBuilder.url(redirectUrl);
      return getInputStream();
    }

    inputStream = responseBody.byteStream();
    bufferedInputStream = new BufferedInputStream(inputStream);
    return bufferedInputStream;
  }

  public JsonReader getJson() throws IOException {
    if (jsonReader != null) {
      return jsonReader;
    }

    stringReader = new StringReader(getString());
    jsonReader = new JsonReader(stringReader);
    return jsonReader;
  }

  public void printJsonMessages() throws IOException {
    // just ensuring we have the reader, in case something other than getJson() was called to send the request.
    getJson();

    jsonReader.beginObject();
    while (jsonReader.hasNext()) {
      switch (jsonReader.nextName()) {
        case "message":
          printMessage(jsonReader.nextString());
          break;
        case "messages":
          jsonReader.beginArray();
          while (jsonReader.hasNext()) {
            printMessage(jsonReader.nextString());
          }
          jsonReader.endArray();
          break;
      default:
        // Just ignore anything else
        jsonReader.skipValue();
        break;
      }
    }
    jsonReader.endObject();
  }

  public void printJsonMessages(String initialMessage) throws IOException {
    printMessage(initialMessage);

    printJsonMessages();
  }

  public Headers getResponseHeaders() {
    return response.headers();
  }

  public int getStatusCode() {
    return response.code();
  }

  public void closeReaders() {
    if (inputStream != null) {
      try {
        inputStream.close();
      } catch (IOException e) {
        // do nothing, maybe it was just already closed?
      }
    }

    if (jsonReader != null) {
      try {
        jsonReader.close();
      } catch (IOException e) {
        // do nothing, maybe it was just already closed?
      }
    }

    // Ensure this happens AFTER json reader, since the json reader is what actually uses it.
    // We do want to make sure we close it though.
    if (stringReader != null) {
      stringReader.close();
    }

    // Make sure we actually close out the connection.
    if (response != null) {
      response.close();
    }
  }

  public String userAgent() {
    return "SandboxMC Agent (" + MOD_VERSION + "); Minecraft (" + source.getServer().getVersion() + "); Java (" + JavaVersion.getMajorJavaVersion() + ");";
  }

  public void printMessage(String feedbackText) {
    source.sendFeedback(() -> {
      return Text.literal(feedbackText);
    }, false);
  }
}
