package io.sandboxmc.datapacks;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.sandboxmc.datapacks.types.DatapackMeta;

public class Datapack {
  DatapackMeta datapackMeta = new DatapackMeta();
  Path datapackPath;
  String name;

  public Datapack(Path datapackPath, String name) {
    this.datapackPath = Paths.get(datapackPath.toString(), name);
    this.name = name;
  }

  public void addFolder(String folderPath) {
    System.out.println(folderPath);
    File newFolder = Paths.get(this.datapackPath.toString(), folderPath).toFile();
    if (!newFolder.exists()) {
      newFolder.mkdirs();
    }
  }

  public void saveDatapack() {
    File datapackFolder = this.datapackPath.toFile();
    if (!datapackFolder.exists()) {
      // only create it if it doesn't exist
      datapackFolder.mkdirs();
    }

    Path packMcmetaPath = Paths.get(this.datapackPath.toString(), "pack.mcmeta");
    try {
      FileWriter file = new FileWriter(packMcmetaPath.toString());
      file.write(this.datapackMeta.convertToJsonString());
      file.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
