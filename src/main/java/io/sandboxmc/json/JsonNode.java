package io.sandboxmc.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.google.gson.stream.JsonReader;

import io.sandboxmc.Plunger;

public class JsonNode {
  private JsonNodeType type; // https://json-schema.org/understanding-json-schema/reference/type
  private String valStr;
  private boolean valBool;
  private int valInt;
  private double valDouble;
  private ArrayList<JsonNode> valArray;
  private HashMap<String, JsonNode> valObject;

  public JsonNode(JsonReader jsonReader) {
    injestFromReader(jsonReader);
  }

  public JsonNode(JsonNodeType theType) {
    type = theType;

    switch (type) {
      case OBJECT:
        valObject = new HashMap<>();
        break;
      case ARRAY:
        valArray = new ArrayList<>();
        break;
      default:
        // I don't think we have to do anything special for any of the other ones
        break;
    }
  }

  public JsonNode(String value) {
    this(JsonNodeType.STRING);

    valStr = value;
  }

  public JsonNode(int value) {
    this(JsonNodeType.INTEGER);

    valInt = value;
  }

  public JsonNode(double value) {
    this(JsonNodeType.NUMBER);

    valDouble = value;
  }

  public JsonNode(boolean value) {
    this(JsonNodeType.BOOLEAN);

    valBool = value;
  }

  public JsonNode(ArrayList<JsonNode> value) {
    this(JsonNodeType.ARRAY);

    valArray = value;
  }

  public JsonNode(HashMap<String, JsonNode> value) {
    this(JsonNodeType.OBJECT);

    valObject = value;
  }

  public JsonNodeType getType() {
    return type;
  }

  public Boolean isNull() {
    return type == null;
  }

  public Object getValue() {
    switch (type) {
      case STRING:
        return getString();
      case BOOLEAN:
        return getBoolean();
      case INTEGER:
        return getInteger();
      case NUMBER:
        return getDouble();
      case ARRAY:
        return getArray();
      case OBJECT:
        return getObject();
      default:
        return null;
    }
  }

  public void clearData() {
    valStr = null;
    valBool = false;
    valInt = -1;
    valDouble = -1;
    valArray = null;
    valObject = null;
  }

  public String getString() {
    return valStr;
  }

  public void setString(String value) {
    clearData();
    type = JsonNodeType.STRING;
    valStr = value;
  }

  public boolean getBoolean() {
    return valBool;
  }

  public void setBoolean(boolean value) {
    clearData();
    type = JsonNodeType.BOOLEAN;
    valBool = value;
  }

  public int getInteger() {
    return valInt;
  }

  public void setInteger(int value) {
    clearData();
    type = JsonNodeType.INTEGER;
    valInt = value;
  }

  public double getDouble() {
    return type == JsonNodeType.INTEGER ? valInt : valDouble;
  }

  public void setDouble(double value) {
    clearData();
    type = JsonNodeType.NUMBER;
    valDouble = value;
  }

  public ArrayList<JsonNode> getArray() {
    return valArray;
  }

  public void setArray(ArrayList<JsonNode> value) {
    clearData();
    type = JsonNodeType.ARRAY;
    valArray = value;
  }

  public HashMap<String, JsonNode> getObject() {
    return valObject;
  }

  public void setObject(HashMap<String, JsonNode> value) {
    clearData();
    type = JsonNodeType.OBJECT;
    valObject = value;
  }

  // This is just to be called as if the node was a TOP LEVEL node.
  // This will always signify that there is nothing following this item
  // and no trailing comma will be appended to the resulting string.
  public String toString() {
    return toString(0, false);
  }

  public JsonNode getNode(String key) {
    if (type != JsonNodeType.OBJECT) {
      Plunger.error("Attempted to get from JsonNode but the node was not an object!");
      return null;
    }

    return valObject.get(key);
  }

  public JsonNode put(String key, JsonNodeType theType) {
    if (type != JsonNodeType.OBJECT) {
      Plunger.error("Attempted to put to JsonNode but the node was not an object!");
      return null;
    }

    JsonNode newNode = new JsonNode(theType);
    valObject.put(key, newNode);
    return newNode;
  }

  public JsonNode put(String key, String value) {
    JsonNode newNode = put(key, JsonNodeType.STRING);
    newNode.setString(value);
    return newNode;
  }

  public JsonNode put(String key, int value) {
    JsonNode newNode = put(key, JsonNodeType.INTEGER);
    newNode.setInteger(value);
    return newNode;
  }

  public JsonNode put(String key, double value) {
    JsonNode newNode = put(key, JsonNodeType.NUMBER);
    newNode.setDouble(value);
    return newNode;
  }

  public JsonNode put(String key, boolean value) {
    JsonNode newNode = put(key, JsonNodeType.BOOLEAN);
    newNode.setBoolean(value);
    return newNode;
  }

  private boolean isValidArray() {
    if (type == JsonNodeType.ARRAY) {
      return true;
    }

    Plunger.error("Attempted to add an item but the node was not an array!");
    return false;
  }

  public JsonNode add(String value) {
    if (!isValidArray()) return null;

    JsonNode newNode = new JsonNode(value);
    valArray.add(newNode);
    return newNode;
  }

  public JsonNode add(int value) {
    if (!isValidArray()) return null;

    JsonNode newNode = new JsonNode(value);
    valArray.add(newNode);
    return newNode;
  }

  public JsonNode add(double value) {
    if (!isValidArray()) return null;

    JsonNode newNode = new JsonNode(value);
    valArray.add(newNode);
    return newNode;
  }

  public JsonNode add(boolean value) {
    if (!isValidArray()) return null;

    JsonNode newNode = new JsonNode(value);
    valArray.add(newNode);
    return newNode;
  }

  public JsonNode addArray() {
    if (!isValidArray()) return null;

    JsonNode newNode = new JsonNode(JsonNodeType.ARRAY);
    valArray.add(newNode);
    return newNode;
  }

  public JsonNode addObject() {
    if (!isValidArray()) return null;

    JsonNode newNode = new JsonNode(JsonNodeType.OBJECT);
    valArray.add(newNode);
    return newNode;
  }

  public JsonNode remove(String key) {
    if (type == JsonNodeType.OBJECT) {
      return valObject.remove(key);
    } else if (type == JsonNodeType.ARRAY) {
      // This is part of this method so we can reuse the name
      // removing anything that is NOT a String will only work for arrays
      int idx = valArray.indexOf(new JsonNode(key));
      return idx > -1 ? valArray.remove(idx) : null;
    }

    Plunger.error("Attempted to remove an item but the node was not an array or object!");
    return null;
  }

  public JsonNode remove(int value) {
    if (type != JsonNodeType.ARRAY) {
      Plunger.error("Attempted to remove an item at an index but the node was not an array!");
      return null;
    }

    int idx = valArray.indexOf(new JsonNode(value));
    return idx > -1 ? valArray.remove(idx) : null;
  }

  public JsonNode remove(double value) {
    if (type != JsonNodeType.ARRAY) {
      Plunger.error("Attempted to remove an item at an index but the node was not an array!");
      return null;
    }

    int idx = valArray.indexOf(new JsonNode(value));
    return idx > -1 ? valArray.remove(idx) : null;
  }

  public JsonNode removeAt(int idx) {
    if (type != JsonNodeType.ARRAY) {
      Plunger.error("Attempted to remove an item at an index but the node was not an array!");
      return null;
    }

    return valArray.remove(idx);
  }

  private String toString(int nestLevel, boolean hasNext) {
    StringBuilder sb = new StringBuilder();

    switch (type) {
      case OBJECT:
        sb.append("{\n");
        Iterator<Map.Entry<String, JsonNode>> objectIterator = getObject().entrySet().iterator();
        while (objectIterator.hasNext()) {
          Map.Entry<String, JsonNode> entry = objectIterator.next();
          String key = entry.getKey();
          JsonNode value = entry.getValue();
          for (int i = 0; i < (nestLevel + 1) * 2; i++) sb.append(' ');
          sb.append("\"" + key + "\": " + value.toString(nestLevel + 1, objectIterator.hasNext()));
        }
        for (int i = 0; i < nestLevel * 2; i++) sb.append(' ');
        sb.append("}");
        break;
      case ARRAY:
        sb.append("[\n");
        Iterator<JsonNode> arrayIterator = getArray().iterator();
        while (arrayIterator.hasNext()) {
          JsonNode value = arrayIterator.next();
          for (int i = 0; i < (nestLevel + 1) * 2; i++) sb.append(' ');
          sb.append(value.toString(nestLevel + 1, arrayIterator.hasNext()));
        }
        for (int i = 0; i < nestLevel * 2; i++) sb.append(' ');
        sb.append("]");
        break;
      case STRING:
        sb.append("\"" + valStr + "\"");
        break;
      default:
        if (isNull()) {
          sb.append("null");
        } else {
          sb.append(getValue());
        }
    }

    if (hasNext) {
      sb.append(",");
    }
    sb.append("\n");

    return sb.toString();
  }

  private void injestFromReader(JsonReader jsonReader) {
    try {
      switch (jsonReader.peek()) {
        case STRING:
          type = JsonNodeType.STRING;
          valStr = jsonReader.nextString();
          break;
        case NUMBER:
          Double nextDouble = jsonReader.nextDouble();
          if (nextDouble.longValue() == nextDouble) {
            type = JsonNodeType.INTEGER;
            valInt = nextDouble.intValue();
          } else {
            type = JsonNodeType.NUMBER;
            valDouble = nextDouble;
          }
          break;
        case BOOLEAN:
          type = JsonNodeType.BOOLEAN;
          valBool = jsonReader.nextBoolean();
          break;
        case NULL:
          // A type of null signifies that the value is also null.
          // I currently don't have a good way to know what type it SHOULD have been had it been present.
          type = null;
          jsonReader.skipValue();
        case BEGIN_ARRAY:
          type = JsonNodeType.ARRAY;
          jsonReader.beginArray();
          valArray = new ArrayList<>();
          while (jsonReader.hasNext()) {
            valArray.add(new JsonNode(jsonReader));
          }
          jsonReader.endArray();
          break;
        case BEGIN_OBJECT:
          type = JsonNodeType.OBJECT;
          jsonReader.beginObject();
          valObject = new HashMap<>();
          while (jsonReader.hasNext()) {
            String mapName = jsonReader.nextName();
            valObject.put(mapName, new JsonNode(jsonReader));
          }
          jsonReader.endObject();
          break;
        default:
          Plunger.debug("Unhandled JsonToken type: " + jsonReader.peek());
          break;
      }
    } catch (IOException e) {
      Plunger.error("Failed parsing JSON", e);
    }
  }
}
