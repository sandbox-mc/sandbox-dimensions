package io.sandboxmc.json;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;

import com.google.gson.stream.JsonReader;

import io.sandboxmc.Plunger;

public class JsonConfigItem {
  private static final String[] SUPPORTED_TYPES = new String[]{"STRING", "INTEGER", "ARRAY.STRING", "ARRAY.INTEGER", "MAP.STRING", "MAP.INTEGER"};

  private String type = "STRING";
  private String rawValue = null;
  private String stringVal = null;
  private Integer integerVal = null;
  private ArrayList<String> stringAryVal = null;
  private ArrayList<Integer> integerAryVal = null;
  private HashMap<String, JsonConfigItem> stringMapVal = null;
  private HashMap<Integer, JsonConfigItem> integerMapVal = null;

  public JsonConfigItem(String theType, String theRawValue) {
    type = theType.toUpperCase();
    boolean found = false;
    for (String supportedType : SUPPORTED_TYPES) {
      if (type == supportedType) {
        found = true;
        break;
      }
    }
    if (!found) {
      Plunger.error("Invalid JSON type: " + type);
      return;
    }

    rawValue = theRawValue;
    parseValueFromRaw();
  }

  public Object getValue() {
    switch (type) {
      case "STRING":
        return getString();
      case "INTEGER":
        return getInteger();
      case "ARRAY.STRING":
        return getStringAry();
      case "ARRAY.INTEGER":
        return getIntegerAry();
      case "MAP.STRING":
        return getStringMap();
      case "MAP.INTEGER":
        return getIntegerMap();
      default:
        return null;
    }
  }

  public String getString() {
    return stringVal;
  }

  public Integer getInteger() {
    return integerVal;
  }

  public ArrayList<String> getStringAry() {
    return stringAryVal;
  }

  public ArrayList<Integer> getIntegerAry() {
    return integerAryVal;
  }

  public HashMap<String, JsonConfigItem> getStringMap() {
    return stringMapVal;
  }
  public HashMap<Integer, JsonConfigItem> getIntegerMap() {
    return integerMapVal;
  }

  private void parseValueFromRaw() {
    switch (type) {
      case "STRING":
        stringVal = rawValue;
        break;
      case "INTEGER":
        integerVal = Integer.parseInt(rawValue);
        break;
      case "ARRAY.STRING":
        stringAryVal = parseStringArray(rawValue);
        break;
      case "ARRAY.INTEGER":
        integerAryVal = parseIntegerArray(rawValue);
        break;
      case "MAP.STRING":

        break;
      case "MAP.INTEGER":

        break;
      default:
        break;
    }
  }

  private ArrayList<String> parseStringArray(String toParse) {
    ArrayList<String> ary = new ArrayList<>();
    StringReader stringReader = new StringReader(toParse);
    JsonReader jsonReader = new JsonReader(stringReader);
    try {
      jsonReader.beginArray();
      while (jsonReader.hasNext()) {
        ary.add(jsonReader.nextString());
      }
      jsonReader.endArray();
    } catch (IOException e) {
      Plunger.error("Failed to parse ARRAY.STRING: '" + toParse + "'");
      ary = null;
    } finally {
      try {
        stringReader.close();
        jsonReader.close();
      } catch (IOException e) {
        // ignore, we just need to ensure we close the readers
      }
    }
    return ary;
  }

  private ArrayList<Integer> parseIntegerArray(String toParse) {
    ArrayList<Integer> ary = new ArrayList<>();
    StringReader stringReader = new StringReader(toParse);
    JsonReader jsonReader = new JsonReader(stringReader);
    try {
      jsonReader.beginArray();
      while (jsonReader.hasNext()) {
        ary.add(jsonReader.nextInt());
      }
      jsonReader.endArray();
    } catch (IOException e) {
      Plunger.error("Failed to parse ARRAY.INTEGER: '" + toParse + "'");
      ary = null;
    } finally {
      try {
        stringReader.close();
        jsonReader.close();
      } catch (IOException e) {
        // ignore, we just need to ensure we close the readers
      }
    }
    return ary;
  }
}
