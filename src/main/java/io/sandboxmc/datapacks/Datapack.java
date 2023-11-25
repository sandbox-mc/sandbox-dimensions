package io.sandboxmc.datapacks;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

import io.sandboxmc.datapacks.types.DatapackMeta;
import io.sandboxmc.dimension.DimensionSave;

public class Datapack {
  DatapackMeta datapackMeta = new DatapackMeta();
  Path datapackPath;
  String name;

  public Datapack(Path datapackPath, String name) {
    this.datapackPath = Paths.get(datapackPath.toString(), name);
    this.name = name;
  }

  public void addDimensionFile(String namespace, String dimensionName, InputStream fileStream) {
    Path dimensionConfigPath = Paths.get("data", namespace, "dimension");
    this.addFolder(dimensionConfigPath.toString());
    dimensionConfigPath = Paths.get(datapackPath.toString(), dimensionConfigPath.toString(), dimensionName + ".json");

    // Create new dimension.json file for the new dimension
    // This will allow for reloading this world
    try {
      Files.copy(fileStream, dimensionConfigPath, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public void addFolder(String folderPath) {
    System.out.println(folderPath);
    File newFolder = Paths.get(this.datapackPath.toString(), folderPath).toFile();
    if (!newFolder.exists()) {
      newFolder.mkdirs();
    }
  }

  public void addWorldSaveFile(String namespace, String dimensionName, InputStream fileStream) {
    Path worldSavePath = Paths.get("data", namespace, DimensionSave.WORLD_SAVE_FOLDER);
    this.addFolder(worldSavePath.toString());
    worldSavePath = Paths.get(datapackPath.toString(), worldSavePath.toString(), dimensionName + ".zip");

    // Copy over a default world save to use as default
    // This is the save file that will be used for the new dimension
    try {
      Files.copy(fileStream, worldSavePath, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      e.printStackTrace();
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
