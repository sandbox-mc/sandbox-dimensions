package io.sandboxmc.json;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.stream.JsonReader;

import io.sandboxmc.Plunger;

public class JsonContainer {
  private JsonNode topNode = null;
  private File file = null;
  private Path filePath = null;

  public JsonContainer() {}

  public JsonContainer(File theFile) {
    filePath = theFile.toPath();
    file = theFile;
  }

  public JsonContainer(Path thePath) {
    filePath = thePath;
    file = new File(filePath.toString());
  }

  public File getFile() {
    return file;
  }

  public void setFile(File theFile) {
    filePath = theFile.toPath();
    file = theFile;
  }

  public void loadDataFromFile() {
    BufferedReader reader = null;
    JsonReader jsonReader = null;
    try {
      reader = Files.newBufferedReader(filePath);
      jsonReader = new JsonReader(reader);
      topNode = new JsonNode(jsonReader);
    } catch (IOException e) {
      Plunger.error("Failed to load data from file!", e);
    } finally {
      try {
        if (jsonReader != null) jsonReader.close();
        if (reader != null) reader.close();
      } catch (Exception e) {
        // ignore
      }
    }
  }

  // Only top level key:values
  public JsonNode put(String key, String value) {
    return putAt(new String[]{ key }, value);
  }

  public JsonNode putAt(String[] keys, String value) {
    return putAt(keys, 0, topNode, value);
  }

  public JsonNode putAt(String[] keys, int idx, JsonNode lastNode, String value) {
    lastNode = ensureTopNode(idx, lastNode);
    if (lastNode == null) {
      Plunger.error("Attempted to put to a null node at " + keys.toString() + " - " + keys[idx]);
      return lastNode;
    }

    if (idx == (keys.length - 1)) {
      // last item, we can actually put the value here
      return lastNode.put(keys[idx], value);
    }

    lastNode = lastNode.put(keys[idx], JsonNodeType.OBJECT);
    return putAt(keys, idx + 1, lastNode, value);
  }

  // Only top level key:values
  public JsonNode put(String key, int value) {
    return putAt(new String[]{ key }, value);
  }

  public JsonNode putAt(String[] keys, int value) {
    return putAt(keys, 0, topNode, value);
  }

  public JsonNode putAt(String[] keys, int idx, JsonNode lastNode, int value) {
    lastNode = ensureTopNode(idx, lastNode);
    if (lastNode == null) {
      Plunger.error("Attempted to put to a null node at " + keys.toString() + " - " + keys[idx]);
      return lastNode;
    }

    if (idx == (keys.length - 1)) {
      // last item, we can actually put the value here
      return lastNode.put(keys[idx], value);
    }

    lastNode = lastNode.put(keys[idx], JsonNodeType.OBJECT);
    return putAt(keys, idx + 1, lastNode, value);
  }

  // Only top level key:values
  public JsonNode put(String key, double value) {
    return putAt(new String[]{ key }, value);
  }

  public JsonNode putAt(String[] keys, double value) {
    return putAt(keys, 0, topNode, value);
  }

  public JsonNode putAt(String[] keys, int idx, JsonNode lastNode, double value) {
    lastNode = ensureTopNode(idx, lastNode);
    if (lastNode == null) {
      Plunger.error("Attempted to put to a null node at " + keys.toString() + " - " + keys[idx]);
      return lastNode;
    }

    if (idx == (keys.length - 1)) {
      // last item, we can actually put the value here
      return lastNode.put(keys[idx], value);
    }

    lastNode = lastNode.put(keys[idx], JsonNodeType.OBJECT);
    return putAt(keys, idx + 1, lastNode, value);
  }

  // Only top level key:values
  public JsonNode put(String key, boolean value) {
    return putAt(new String[]{ key }, value);
  }

  public JsonNode putAt(String[] keys, boolean value) {
    return putAt(keys, 0, topNode, value);
  }

  public JsonNode putAt(String[] keys, int idx, JsonNode lastNode, boolean value) {
    lastNode = ensureTopNode(idx, lastNode);
    if (lastNode == null) {
      Plunger.error("Attempted to put to a null node at " + keys.toString() + " - " + keys[idx]);
      return lastNode;
    }

    if (idx == (keys.length - 1)) {
      // last item, we can actually put the value here
      return lastNode.put(keys[idx], value);
    }

    lastNode = lastNode.put(keys[idx], JsonNodeType.OBJECT);
    return putAt(keys, idx + 1, lastNode, value);
  }

  // "putAt" for type OBJECT is redundant,
  // you don't need to instantiate it until you're putting the first key.

  // TODO:TYLER implement addAt for Arrays
  // TODO:TYLER implement removeAt for both Objects and Arrays

  private JsonNode ensureTopNode(int idx, JsonNode lastNode) {
    if (idx != 0 || topNode != null) {
      return lastNode;
    }

    if (lastNode == null) {
      // In this case we're actually putting to an uninstantiated top level node
      lastNode = topNode = new JsonNode(JsonNodeType.OBJECT);
    } else {
      // We had no top node but they've given us one
      // This only happens if someone calls this method directly 
      // with an already instantiated top node for some reason.
      topNode = lastNode;
    }

    return lastNode;
  }

  public String toString() {
    if (topNode == null) {
      Plunger.debug("Had no top level node!");
      return "";
    }

    switch (topNode.getType()) {
      case OBJECT:
        return topNode.toString();
      case ARRAY:
        Plunger.error("Top level JSON nodes of Array are not currently supported.");
        break;
      default:
        Plunger.error("Cannot have a top level node of " + topNode.getType() + " for JSON container.");
    }

    return "";
  }
}
