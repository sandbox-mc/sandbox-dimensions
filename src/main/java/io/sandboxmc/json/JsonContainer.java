package io.sandboxmc.json;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.stream.JsonReader;

import io.sandboxmc.Plunger;

public class JsonContainer {
  private JsonNode topNode = null;
  private File file = null;
  private Path filePath = null;

  public static void injestExample(File theFile) {
    JsonContainer jsonConfig = new JsonContainer(theFile);
    jsonConfig.loadDataFromFile();
  }

  public static void buildAndWriteExample(File theFile) {
    JsonContainer container = new JsonContainer(theFile);
    container.put("base string value", "Some string at the top level");
    container.putAt(new String[]{"object key", "inner object key", "integer value"}, 5);
    container.putAt(new String[]{"object key", "inner object key", "double value"}, 500.456);
    container.putAt(new String[]{"object key", "inner object key", "boolean value"}, true);
    Plunger.debug("Outputting JSON:\n" + container.toString());
    Integer intVal = (Integer)container.getAt(new String[]{"object key", "inner object key", "integer value"});
    Plunger.debug("Accessing 'object key'.'inner object key'.'integer value': " + intVal);
    Plunger.debug("Writing to file at: " + container.getFile().toPath());
    container.writeDataToFile();
  }

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
      } catch (Exception e) {}
    }
  }

  public boolean writeDataToFile() {
    if (file == null) {
      Plunger.error("No file given to write JSON to!");
      return false;
    }
    BufferedWriter writer = null;

    try {
      file.delete(); // don't append to existing file...
      writer = new BufferedWriter(new FileWriter(file));
      writer.write(toString());
      return true;
    } catch (IOException e) {
      Plunger.error("Failed to write JSON to file!", e);
    } finally {
      if (writer != null) {
        try { writer.close(); } catch (IOException e) {}
      }
    }

    return false;
  }

  public boolean writeDataToFile(File theFile) {
    file = theFile;
    filePath = file.toPath();

    return writeDataToFile();
  }

  // Top level only
  public JsonNode getNode(String key) {
    return getNodeAt(new String[] { key });
  }

  public JsonNode getNodeAt(String[] keys) {
    return getNodeAt(keys, 0, topNode);
  }

  public JsonNode getNodeAt(String[] keys, int idx, JsonNode lastNode) {
    lastNode = ensureTopNode(idx, lastNode);
    if (lastNode == null) return null; // Hit a dead end, early return

    JsonNode nextNode = lastNode.getNode(keys[idx]);

    if (idx == (keys.length - 1)) {
      // last item, we can return the node now
      return nextNode;
    }

    // Not at the last item yet, do recursion
    return getNodeAt(keys, idx + 1, nextNode);
  }

  // Top level only
  public Object get(String key) {
    return getAt(new String[] { key });
  }

  public Object getAt(String[] keys) {
    JsonNode node = getNodeAt(keys, 0, topNode);
    return node == null ? null : node.getValue();
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
    if (!isValidNode(keys, idx, lastNode)) return null;

    if (idx == (keys.length - 1)) {
      // last item, we can actually put the value here
      return lastNode.put(keys[idx], value);
    }

    return putAt(keys, idx + 1, getNextNode(keys[idx], lastNode), value);
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
    if (!isValidNode(keys, idx, lastNode)) return null;

    if (idx == (keys.length - 1)) {
      // last item, we can actually put the value here
      return lastNode.put(keys[idx], value);
    }

    return putAt(keys, idx + 1, getNextNode(keys[idx], lastNode), value);
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
    if (!isValidNode(keys, idx, lastNode)) return null;

    if (idx == (keys.length - 1)) {
      // last item, we can actually put the value here
      return lastNode.put(keys[idx], value);
    }

    return putAt(keys, idx + 1, getNextNode(keys[idx], lastNode), value);
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
    if (!isValidNode(keys, idx, lastNode)) return null;

    if (idx == (keys.length - 1)) {
      // last item, we can actually put the value here
      return lastNode.put(keys[idx], value);
    }

    return putAt(keys, idx + 1, getNextNode(keys[idx], lastNode), value);
  }

  // Only top level key:values
  public JsonNode putArray(String key) {
    return putArrayAt(new String[]{ key });
  }

  public JsonNode putArrayAt(String[] keys) {
    return putArrayAt(keys, 0, topNode);
  }

  public JsonNode putArrayAt(String[] keys, int idx, JsonNode lastNode) {
    lastNode = ensureTopNode(idx, lastNode);
    if (!isValidNode(keys, idx, lastNode)) return null;

    if (idx == (keys.length - 1)) {
      // last item, we can actually put the value here
      return lastNode.put(keys[idx], JsonNodeType.ARRAY);
    }

    return putArrayAt(keys, idx + 1, getNextNode(keys[idx], lastNode));
  }

  // "putAt" for type OBJECT is redundant,
  // you don't need to instantiate it until you're putting the first key.

  // Only top level key:values
  public JsonNode addAt(String key, String value) {
    return addAt(new String[]{ key }, value);
  }

  public JsonNode addAt(String[] keys, String value) {
    return addAt(keys, 0, topNode, value);
  }

  public JsonNode addAt(String[] keys, int idx, JsonNode lastNode, String value) {
    lastNode = ensureTopNode(idx, lastNode);
    if (!isValidNode(keys, idx, lastNode)) return null;

    // valid node, let's get the next...
    JsonNode nextNode = getNextNode(keys[idx], lastNode);

    if (idx == (keys.length - 1)) {
      // now we're the last node, we can try adding the value
      if (nextNode == null) {
        // if it's null we can insert an empty array
        nextNode = putArrayAt(keys, idx, lastNode);
      }
      return nextNode.add(value);
    }

    return addAt(keys, idx + 1, nextNode, value);
  }

  // Only top level key:values
  public JsonNode addAt(String key, int value) {
    return addAt(new String[]{ key }, value);
  }

  public JsonNode addAt(String[] keys, int value) {
    return addAt(keys, 0, topNode, value);
  }

  public JsonNode addAt(String[] keys, int idx, JsonNode lastNode, int value) {
    lastNode = ensureTopNode(idx, lastNode);
    if (!isValidNode(keys, idx, lastNode)) return null;

    // valid node, let's get the next...
    JsonNode nextNode = getNextNode(keys[idx], lastNode);

    if (idx == (keys.length - 1)) {
      // now we're the last node, we can try adding the value
      if (nextNode == null) {
        // if it's null we can insert an empty array
        nextNode = putArrayAt(keys, idx, lastNode);
      }
      return nextNode.add(value);
    }

    return addAt(keys, idx + 1, nextNode, value);
  }

  // Only top level key:values
  public JsonNode addAt(String key, double value) {
    return addAt(new String[]{ key }, value);
  }

  public JsonNode addAt(String[] keys, double value) {
    return addAt(keys, 0, topNode, value);
  }

  public JsonNode addAt(String[] keys, int idx, JsonNode lastNode, double value) {
    lastNode = ensureTopNode(idx, lastNode);
    if (!isValidNode(keys, idx, lastNode)) return null;

    // valid node, let's get the next...
    JsonNode nextNode = getNextNode(keys[idx], lastNode);

    if (idx == (keys.length - 1)) {
      // now we're the last node, we can try adding the value
      if (nextNode == null) {
        // if it's null we can insert an empty array
        nextNode = putArrayAt(keys, idx, lastNode);
      }
      return nextNode.add(value);
    }

    return addAt(keys, idx + 1, nextNode, value);
  }

  // Only top level key:values
  public JsonNode addAt(String key, boolean value) {
    return addAt(new String[]{ key }, value);
  }

  public JsonNode addAt(String[] keys, boolean value) {
    return addAt(keys, 0, topNode, value);
  }

  public JsonNode addAt(String[] keys, int idx, JsonNode lastNode, boolean value) {
    lastNode = ensureTopNode(idx, lastNode);
    if (!isValidNode(keys, idx, lastNode)) return null;

    // valid node, let's get the next...
    JsonNode nextNode = getNextNode(keys[idx], lastNode);

    if (idx == (keys.length - 1)) {
      // now we're the last node, we can try adding the value
      if (nextNode == null) {
        // if it's null we can insert an empty array
        nextNode = putArrayAt(keys, idx, lastNode);
      }
      return nextNode.add(value);
    }

    return addAt(keys, idx + 1, nextNode, value);
  }

  // Only top level key:values
  public JsonNode addArrayAt(String key) {
    return addArrayAt(new String[]{ key });
  }

  public JsonNode addArrayAt(String[] keys) {
    return addArrayAt(keys, 0, topNode);
  }

  public JsonNode addArrayAt(String[] keys, int idx, JsonNode lastNode) {
    lastNode = ensureTopNode(idx, lastNode);
    if (!isValidNode(keys, idx, lastNode)) return null;

    // valid node, let's get the next...
    JsonNode nextNode = getNextNode(keys[idx], lastNode);

    if (idx == (keys.length - 1)) {
      // now we're the last node, we can try adding the value
      if (nextNode == null) {
        // if it's null we can insert an empty array
        nextNode = putArrayAt(keys, idx, lastNode);
      }
      return nextNode.addArray();
    }

    return addArrayAt(keys, idx + 1, nextNode);
  }

  // Only top level key:values
  public JsonNode addObjectAt(String key) {
    return addObjectAt(new String[]{ key });
  }

  public JsonNode addObjectAt(String[] keys) {
    return addObjectAt(keys, 0, topNode);
  }

  public JsonNode addObjectAt(String[] keys, int idx, JsonNode lastNode) {
    lastNode = ensureTopNode(idx, lastNode);
    if (!isValidNode(keys, idx, lastNode)) return null;

    // valid node, let's get the next...
    JsonNode nextNode = getNextNode(keys[idx], lastNode);

    if (idx == (keys.length - 1)) {
      // now we're the last node, we can try adding the value
      if (nextNode == null) {
        // if it's null we can insert an empty array
        nextNode = putArrayAt(keys, idx, lastNode);
      }
      return nextNode.addObject();
    }

    return addObjectAt(keys, idx + 1, nextNode);
  }

  // Only top level keys
  public JsonNode remove(String key) {
    return removeAt(new String[]{ key });
  }

  public JsonNode removeAt(String[] keys) {
    return removeAt(keys, 0, topNode);
  }

  public JsonNode removeAt(String[] keys, int idx, JsonNode lastNode) {
    lastNode = ensureTopNode(idx, lastNode);
    if (lastNode == null) return null;

    if (idx == (keys.length - 1)) {
      return lastNode.remove(keys[idx]);
    }

    return removeAt(keys, idx + 1, getNextNode(keys[idx], lastNode));
  }

  // Only top level
  public JsonNode removeFromArray(String key, String value) {
    return removeFromArrayAt(new String[] { key }, value);
  }

  public JsonNode removeFromArrayAt(String[] keys, String value) {
    return removeFromArrayAt(keys, 0, topNode, value);
  }
  
  public JsonNode removeFromArrayAt(String[] keys, int idx, JsonNode lastNode, String value) {
    lastNode = ensureTopNode(idx, lastNode);
    JsonNode nextNode = getNextNode(keys[idx], lastNode);
    if (nextNode == null) return null;

    if (idx == (keys.length - 1)) {
      return nextNode.remove(value);
    }

    return removeFromArrayAt(keys, idx + 1, nextNode, value);
  }

  // Only top level
  public JsonNode removeFromArray(String key, int value) {
    return removeFromArrayAt(new String[] { key }, value);
  }

  public JsonNode removeFromArrayAt(String[] keys, int value) {
    return removeFromArrayAt(keys, 0, topNode, value);
  }
  
  public JsonNode removeFromArrayAt(String[] keys, int idx, JsonNode lastNode, int value) {
    lastNode = ensureTopNode(idx, lastNode);
    JsonNode nextNode = getNextNode(keys[idx], lastNode);
    if (nextNode == null) return null;

    if (idx == (keys.length - 1)) {
      return nextNode.remove(value);
    }

    return removeFromArrayAt(keys, idx + 1, nextNode, value);
  }

  // Only top level
  public JsonNode removeFromArray(String key, double value) {
    return removeFromArrayAt(new String[] { key }, value);
  }

  public JsonNode removeFromArrayAt(String[] keys, double value) {
    return removeFromArrayAt(keys, 0, topNode, value);
  }
  
  public JsonNode removeFromArrayAt(String[] keys, int idx, JsonNode lastNode, double value) {
    lastNode = ensureTopNode(idx, lastNode);
    JsonNode nextNode = getNextNode(keys[idx], lastNode);
    if (nextNode == null) return null;

    if (idx == (keys.length - 1)) {
      return nextNode.remove(value);
    }

    return removeFromArrayAt(keys, idx + 1, nextNode, value);
  }

  // Only top level
  public JsonNode removeFromArray(String key, boolean value) {
    return removeFromArrayAt(new String[] { key }, value);
  }

  public JsonNode removeFromArrayAt(String[] keys, boolean value) {
    return removeFromArrayAt(keys, 0, topNode, value);
  }
  
  public JsonNode removeFromArrayAt(String[] keys, int idx, JsonNode lastNode, boolean value) {
    lastNode = ensureTopNode(idx, lastNode);
    JsonNode nextNode = getNextNode(keys[idx], lastNode);
    if (nextNode == null) return null;

    if (idx == (keys.length - 1)) {
      return nextNode.remove(value);
    }

    return removeFromArrayAt(keys, idx + 1, nextNode, value);
  }

  private JsonNode ensureTopNode(int idx, JsonNode lastNode) {
    // If we're NOT the first node or if the top node isn't null then just leave
    // This method is only trying to make sure we have a top node
    if (idx != 0 || topNode != null) return lastNode;

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

  private boolean isValidNode(String[] keys, int idx, JsonNode theNode) {
    if (theNode == null) {
      Plunger.error("Attempted to put to a null node at " + keys.toString() + " - " + keys[idx]);
      return false;
    }

    return true;
  }
  
  private JsonNode getNextNode(String key, JsonNode lastNode) {
    if (lastNode == null) return null;

    JsonNode nextNode = lastNode.getNode(key);

    if (nextNode == null || nextNode.getType() != JsonNodeType.OBJECT) {
      return lastNode.put(key, JsonNodeType.OBJECT);
    } else {
      return nextNode;
    }
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
