package io.sandboxmc.json;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.google.gson.stream.JsonReader;

import io.sandboxmc.Plunger;

public class JsonNode {
  private String id = null;
  private int index = -1;
  private String type; // https://json-schema.org/understanding-json-schema/reference/type
  private String valStr;
  private Boolean valBool;
  private int valInt;
  private double valDouble;
  private ArrayList<JsonNode> valArray;
  private HashMap<String, JsonNode> valMap;

  public JsonNode(int theIndex, JsonReader jsonReader) {
    index = theIndex;

    injestFromReader(jsonReader);
  }

  public JsonNode(String key, JsonReader jsonReader) {
    id = key;

    injestFromReader(jsonReader);
  }

  public String getType() {
    return type;
  }

  public String getKey() {
    return id;
  }

  public int getIndex() {
    return index;
  }

  public Boolean isNull() {
    return type == null;
  }

  public Object getValue() {
    switch (type) {
      case "STRING":
        return getString();
      case "BOOLEAN":
        return getBoolean();
      case "INTEGER":
        return getInteger();
      case "NUMBER":
        return getDouble();
      case "ARRAY":
        return getArray();
      case "OBJECT":
        return getMap();
      default:
        return null;
    }
  }

  public String getString() {
    return valStr;
  }

  public Boolean getBoolean() {
    return valBool;
  }

  public int getInteger() {
    return valInt;
  }

  public double getDouble() {
    return type == "INTEGER" ? valInt : valDouble;
  }

  public ArrayList<JsonNode> getArray() {
    return valArray;
  }

  public HashMap<String, JsonNode> getMap() {
    return valMap;
  }

  // This is just to be called as if the node was a TOP LEVEL node.
  // This will always signify that there is nothing following this item
  // and no trailing comma will be appended to the resulting string.
  public String toString() {
    return toString(0, false);
  }

  private String toString(int nestLevel, Boolean hasNext) {
    StringBuilder sb = new StringBuilder();

    switch (type) {
      case "OBJECT":
        sb.append("{\n");
        Iterator<Map.Entry<String, JsonNode>> mapIterator = getMap().entrySet().iterator();
        while (mapIterator.hasNext()) {
          Map.Entry<String, JsonNode> entry = mapIterator.next();
          String key = entry.getKey();
          JsonNode value = entry.getValue();
          for (int i = 0; i < (nestLevel + 1) * 2; i++) sb.append(' ');
          sb.append("\"" + key + "\": " + value.toString(nestLevel + 1, mapIterator.hasNext()));
        }
        for (int i = 0; i < nestLevel * 2; i++) sb.append(' ');
        sb.append("}");
        break;
      case "ARRAY":
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
      case "STRING":
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
          type = "STRING";
          valStr = jsonReader.nextString();
          break;
        case NUMBER:
          Double nextDouble = jsonReader.nextDouble();
          if (nextDouble.longValue() == nextDouble) {
            type = "INTEGER";
            valInt = nextDouble.intValue();
          } else {
            type = "NUMBER";
            valDouble = nextDouble;
          }
          break;
        case BOOLEAN:
          type = "BOOLEAN";
          valBool = jsonReader.nextBoolean();
          break;
        case NULL:
          // A type of null signifies that the value is also null.
          // I currently don't have a good way to know what type it SHOULD have been had it been present.
          type = null;
          jsonReader.skipValue();
        case BEGIN_ARRAY:
          type = "ARRAY";
          jsonReader.beginArray();
          valArray = new ArrayList<>();
          while (jsonReader.hasNext()) {
            valArray.add(new JsonNode(valArray.size(), jsonReader));
          }
          jsonReader.endArray();
          break;
        case BEGIN_OBJECT:
          type = "OBJECT";
          jsonReader.beginObject();
          valMap = new HashMap<>();
          while (jsonReader.hasNext()) {
            String mapName = jsonReader.nextName();
            valMap.put(mapName, new JsonNode(mapName, jsonReader));
          }
          jsonReader.endObject();
          break;
        default:
          break;
      }
    } catch (IOException e) {
      Plunger.error("Failed parsing JSON at KEY: " + (id == null ? "NULL" : id) + ", INDEX: " + index, e);
    }
  }
}
