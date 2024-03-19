package io.sandboxmc.json;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

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

  public void loadDataFromFile() {
    BufferedReader reader = null;
    JsonReader jsonReader = null;
    try {
      reader = Files.newBufferedReader(filePath);
      jsonReader = new JsonReader(reader);
      topNode = new JsonNode("TOP NODE", jsonReader);
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

  public String toString() {
    if (topNode == null) {
      Plunger.debug("Had no top level node!");
      return "";
    }

    switch (topNode.getType()) {
      case "OBJECT":
      case "ARRAY":
        return topNode.toString();
      default:
        Plunger.debug("We had a top level node of type " + topNode.getType() + " which is considered invalid JSON.");
    }

    return "";
  }
}
