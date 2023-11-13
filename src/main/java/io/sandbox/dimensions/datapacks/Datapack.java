package io.sandbox.dimensions.datapacks;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.sandbox.dimensions.datapacks.types.DatapackMeta;

public class Datapack {
  DatapackMeta datapackMeta = new DatapackMeta();
  Path datapackPath;
  String name;

  public Datapack(Path datapackPath, String name) {
    this.datapackPath = Paths.get(datapackPath.toString(), name);
    this.name = name;
  }

  public void createDatapack(String name) {
    File datapackFolder = this.datapackPath.toFile();
    if (datapackFolder.exists()) {
      // if it exists, we should not create it, right?
    }
  }
}
