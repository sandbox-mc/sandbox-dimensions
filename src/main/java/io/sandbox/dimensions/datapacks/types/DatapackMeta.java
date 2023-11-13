package io.sandbox.dimensions.datapacks.types;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class DatapackMeta {
  public Pack pack = new Pack();

  public String convertToJson() {
    // This is pretty print for JSON in the .mcmeta file
    Gson gson = new GsonBuilder().setPrettyPrinting().create();
    return gson.toJson(this);
  }
}
