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
import io.sandboxmc.dimension.DimensionManager;
import io.sandboxmc.dimension.DimensionSave;
import io.sandboxmc.mixin.MinecraftServerAccessor;
import io.sandboxmc.zip.ZipUtility;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;
import net.minecraft.world.level.storage.LevelStorage.Session;

public class Datapack {
  DatapackMeta datapackMeta = new DatapackMeta();
  public Path datapackPath;
  public String name;
  Path tmpDirectory;
  Path tmpFile;

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
      DatapackManager.registerDatapackDimension(this.name, new Identifier(namespace, dimensionName));
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

  public Path createTmpZip() throws IOException {
    // Cleanup any previous tmpDirs
    deleteTmpZipFile();

    // Create and assign tmpDir for serving to the webclient
    this.tmpDirectory = Files.createTempDirectory(datapackPath.getParent(), name);
    this.tmpFile = Paths.get(this.tmpDirectory.toString(), name + ".zip");
    ZipUtility.zipDirectory(this.datapackPath.toFile(), this.tmpFile.toString());

    // Queue tmpfiles to be deleted on server restart
    this.tmpDirectory.toFile().deleteOnExit();
    this.tmpFile.toFile().deleteOnExit();
    return tmpFile;
  }

  public void deleteTmpZipFile() throws IOException {
    if (this.tmpDirectory != null) {
      ZipUtility.deleteDirectory(this.tmpDirectory);
    }
  }

  public void initializeDatapack(MinecraftServer server) {
    File datapackFolder = this.datapackPath.toFile();
    if (!datapackFolder.exists()) {
      // only create it if it doesn't exist
      datapackFolder.mkdirs();
    }

    Path packMcmetaPath = Paths.get(this.datapackPath.toString(), "pack.mcmeta");
    if (!packMcmetaPath.toFile().exists()) {
      // If this doesn't exist we are generating a new datapack and need to create files
      try {
        FileWriter file = new FileWriter(packMcmetaPath.toString());
        file.write(this.datapackMeta.convertToJsonString());
        file.close();
      } catch (IOException e) {
        e.printStackTrace();
      }
    } else {
      // this is a loaded datapack
      // we need to read through the files and process
      File dataFolder = Paths.get(datapackFolder.toString(), "data").toFile();
      File[] namespaces = dataFolder.listFiles((dir, name) -> dir.isDirectory());
      for (File namespaceFile : namespaces) {
        String namespace = namespaceFile.getName();
        File namespaceDimensionFolder = Paths.get(namespaceFile.toString(), "dimension").toFile();
        
        File[] dimensionsInNamespace = namespaceDimensionFolder.listFiles((dir, name) -> {
          return name.endsWith(".json");
        });

        for (File dimensionFile : dimensionsInNamespace) {
          Identifier dimensionIdentifier = new Identifier(
            namespace,
            dimensionFile.getName().replaceAll(".json", "")
          );
  
          // Create the dimension
          DimensionManager.addDimensionToPacknameMap(dimensionIdentifier.toString(), this.name);
          DimensionSave.loadDimensionFile(
            dimensionIdentifier,
            server
          );
          DimensionManager.createDimensionWorld(
            server,
            dimensionIdentifier,
            new Identifier("overworld"),
            server.getWorld(World.OVERWORLD).getSeed()
          );
        }
      }
    }
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
