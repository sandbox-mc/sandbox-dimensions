package io.sandboxmc.datapacks;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import io.sandboxmc.Plunger;
import io.sandboxmc.datapacks.types.DatapackMeta;
import io.sandboxmc.dimension.DimensionManager;
import io.sandboxmc.dimension.DimensionSave;
import io.sandboxmc.dimension.SandboxWorldConfig;
import io.sandboxmc.dimension.configs.DatapackDimensionConfig;
import io.sandboxmc.mixin.MinecraftServerAccessor;
import io.sandboxmc.zip.ZipUtility;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.world.level.storage.LevelStorage.Session;

public class Datapack {
  DatapackMeta datapackMeta = new DatapackMeta();
  private Path datapackPath;
  private Map<Identifier, DatapackDimensionConfig> dimensionsToLoad = new HashMap<>();
  private Map<Identifier, DimensionSave> dimensions = new HashMap<>();
  public String name;
  // private HashSet<String> namespaces = new HashSet<>();
  Path tmpDirectory;
  Path tmpFile;

  public Datapack(String name) {
    if (DatapackManager.getRootPath() != null) {
      this.datapackPath = Paths.get(DatapackManager.getRootPath().toString(), name);
    }

    this.name = name;
  }

  public Datapack addDimension(DimensionSave dimensionSave) {
    Identifier dimensionId = dimensionSave.getIdentifier();
    if (this.dimensions.containsKey(dimensionId)) {
      return this;
    }

    dimensionSave.setDatapackName(this.name);
    dimensionSave.generateConfigFiles();

    this.dimensions.put(dimensionId, dimensionSave);
    return this;
  }

  public void addFolder(String folderPath) {
    Plunger.debug("Adding folder: " + folderPath);
    File newFolder = Paths.get(this.datapackPath.toString(), folderPath).toFile();
    if (!newFolder.exists()) {
      newFolder.mkdirs();
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

  public Path getDatapackPath() {
    return this.datapackPath;
  }

  public Set<Identifier> getDimensionIds() {
    return this.dimensions.keySet();
  }

  public void initializeDatapack() {
    File datapackFolder = this.datapackPath.toFile();
    if (!datapackFolder.exists()) {
      // create it if it doesn't exist
      datapackFolder.mkdirs();
    }

    Path packMcmetaPath = Paths.get(this.datapackPath.toString(), "pack.mcmeta");
    if (!packMcmetaPath.toFile().exists()) {
      // generating a new datapack and need to create files
      try {
        FileWriter file = new FileWriter(packMcmetaPath.toString());
        file.write(this.datapackMeta.convertToJsonString());
        file.close();
      } catch (IOException e) {
        Plunger.error("Failed to Create metadata for: " + this.name, e);
      }
    }
  }

  public void loadQueuedDimensions() {
    Iterator<Entry<Identifier, DatapackDimensionConfig>> iteratorSet = this.dimensionsToLoad.entrySet().iterator();
    while (iteratorSet.hasNext()) {
      Entry<Identifier, DatapackDimensionConfig> entry = iteratorSet.next();

      // Attempt to load and dequeue
      if (this.queueOrReloadDimension(entry.getKey(), entry.getValue())) {
        iteratorSet.remove();
      }
    }
  }

  public Boolean queueOrReloadDimension(Identifier id, DatapackDimensionConfig datapackDimensionConfig) {
    if (datapackDimensionConfig == null) {
      Plunger.error("datapackDimensionConfig cannot be null for dimension: " + id);
      // remove from queue to prevent continuous processing
      return true;
    }

    // Check if id exists
    DimensionSave dimensionSave = DimensionManager.getDimensionSave(id);

    if (dimensionSave == null) {
      if (DatapackManager.getRootPath() == null) {
        // we queue this up to be loaded onServerStart()
        this.dimensionsToLoad.put(id, datapackDimensionConfig);
        return false;
      }

      Plunger.info("Identifier: " + id);

      SandboxWorldConfig sandboxConfig = new SandboxWorldConfig(DatapackManager.getServer(), datapackDimensionConfig);
      dimensionSave = DimensionManager.buildDimensionSaveFromConfig(id, sandboxConfig);
      this.addDimension(dimensionSave);
    }

    return true;
  }

  public void removeDimension(Identifier dimensionId) {
    DimensionSave dimensionSave = DimensionManager.getDimensionSave(dimensionId);
    if (!this.dimensions.containsKey(dimensionId)) {
      Plunger.error("Datapack: " + this.name + " does not contain Dimension: " + dimensionId);
      return;
    }

    // Remove the datapack
    dimensionSave.setDatapackName(null);
    // Generate configs without datapack set should move them
    dimensionSave.generateConfigFiles();

    // Remove from cached list
    this.dimensions.remove(dimensionId);
  }

  public Datapack setDescription(String description) {
    this.datapackMeta.pack.description = description;
    return this;
  }

  public Datapack setPackFormat(Integer format) {
    this.datapackMeta.pack.pack_format = format;
    return this;
  }

  public Datapack setRootPath(Path datapackRootPath) {
    this.datapackPath = Paths.get(datapackRootPath.toString(), this.name);
    return this;
  }

  public void zipWorldfilesToDatapack(ServerWorld dimension) {
    Identifier dimensionId = dimension.getRegistryKey().getValue();
    Session session = ((MinecraftServerAccessor)(dimension.getServer())).getSession();
    Path dimensionPath = session.getWorldDirectory(dimension.getRegistryKey());
    Path worldSavePath = buildWorldSavePath(dimensionId.getNamespace(), dimensionId.getPath());

    Plunger.debug("Zipping files for: " + dimensionId);
    ZipUtility.zipDirectory(dimensionPath.toFile(), worldSavePath.toString());
  }
}
