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
import io.sandboxmc.dimension.zip.ZipUtility;
import io.sandboxmc.mixin.MinecraftServerAccessor;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.level.storage.LevelStorage.Session;

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

      // Register the new dimension
      DatapackManager.registerDatapackDimension(dimensionName, new Identifier(namespace, dimensionName));
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
    Path worldSavePath = buildWorldSavePath(namespace, dimensionName);

    // Copy over a default world save to use as default
    // This is the save file that will be used for the new dimension
    try {
      Files.copy(fileStream, worldSavePath, StandardCopyOption.REPLACE_EXISTING);
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  // Creates the world folder if it doesn't exist and returns .zip path
  private Path buildWorldSavePath(String namespace, String dimensionName) {
    Path worldSavePath = Paths.get("data", namespace, DimensionSave.WORLD_SAVE_FOLDER);
    this.addFolder(worldSavePath.toString());
    return Paths.get(datapackPath.toString(), worldSavePath.toString(), dimensionName + ".zip");
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

  public void zipDatapackFiles() {
    // Should zip everything in the datapack
    // Should return Stream?
    // Basically this is the pre-upload action
    // Example:
    // Player runs: /sb upload datapackName
    // the command will get this data pack and run this command
    // the output will then be uploaded to the site
  }

  public void zipWorldfilesToDatapack(ServerWorld dimension) {
    Identifier dimensionId = dimension.getRegistryKey().getValue();
    Session session = ((MinecraftServerAccessor)(dimension.getServer())).getSession();
    Path dimensionPath = session.getWorldDirectory(dimension.getRegistryKey());
    Path worldSavePath = buildWorldSavePath(dimensionId.getNamespace(), dimensionId.getPath());

    System.out.println("Zipping files for: " + dimensionId);
    ZipUtility.zipDirectory(dimensionPath.toFile(), worldSavePath.toString());
  }
}
