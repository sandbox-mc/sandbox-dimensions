package io.sandboxmc.datapacks;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import io.sandboxmc.Plunger;
import io.sandboxmc.SandboxMC;
import io.sandboxmc.datapacks.types.DownloadedPack;
import io.sandboxmc.dimension.DimensionSave;
import io.sandboxmc.dimension.configs.DatapackDimensionConfig;
import io.sandboxmc.mixin.MinecraftServerAccessor;
import io.sandboxmc.zip.ZipUtility;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.level.storage.LevelStorage.Session;

public class DatapackManager {
  private static Path datapackRootPath;
  private static Map<String, Datapack> datapackCache = new HashMap<>();
  private static Map<Identifier, String> dimensionToDatapackMap = new HashMap<>();
  private static Map<String, DownloadedPack> downloadedDatapacks = new HashMap<>();
  private static Gson gson = new Gson();
  private static MinecraftServer minecraftServer;
  private static Path dimensionStorageDirectory = null;
  private static Path storageDirectory = null;

  public static void init() {
    ResourceManagerHelper.get(ResourceType.SERVER_DATA)
    .registerReloadListener(new SimpleSynchronousResourceReloadListener() {
      @Override
      public Identifier getFabricId() {
        return SandboxMC.id("datapack_loader");
      }

      @Override
      public void reload(ResourceManager manager) {
        // Build a list of Dimensions and datapacks with init configs
        Map<Identifier, Resource> initDimensionConfigs = manager.findResources(
          DimensionSave.DIMENSION_CONFIG_FOLDER,
          path -> true
        );
        for (Identifier initDimensionId : initDimensionConfigs.keySet()) {
          Resource resource = initDimensionConfigs.get(initDimensionId);
          String packName = resource.getResourcePackName().replaceAll("file/", "");

          Datapack datapack = getOrCreateDatapack(packName);
          
          // Example initDimensionId
          // <namespace>:dimensioninit/<dimensionName>.json
          String dimensionIdString = initDimensionId.toString()
            .replaceAll(DimensionSave.DIMENSION_CONFIG_FOLDER + "/", "") // remove folder name
            .replaceAll(".json", ""); // Remove file type
          Identifier dimensioIdentifier = new Identifier(dimensionIdString);
          Plunger.info("Processing: " + packName + " : " + dimensioIdentifier);
          try {
            InputStream inputStream = resource.getInputStream();
            byte[] buffer = new byte[inputStream.available()];
            inputStream.read(buffer);
            inputStream.close();
            String jsonString = new String(buffer, "UTF-8");
            DatapackDimensionConfig dimensionConfig = gson.fromJson(jsonString, DatapackDimensionConfig.class);
            
            datapack.queueOrReloadDimension(dimensioIdentifier, dimensionConfig);
          } catch (IOException e) {
            Plunger.error("IO Error while loading: " + dimensioIdentifier, e);
          } catch (JsonSyntaxException e) {
            Plunger.error("Failed to parse data: " + dimensioIdentifier, e);
          }
        }
      }
    });
  }

  public static void addDatapack(String datapackName, Datapack datapack) {
    datapackCache.put(datapackName, datapack);
  }

  public static void addDownloadedDatapack(Identifier name, Path zipFile) {
    downloadedDatapacks.put(name.toString(), new DownloadedPack(name, zipFile));
  }

  public static Datapack getDatapack(String datapackName) {
    return datapackCache.get(datapackName);
  }

  public static Set<String> getDatapackNames() {
    return datapackCache.keySet();
  }

  public static String getDatapackName(Identifier dimensionId) {
    return dimensionToDatapackMap.get(dimensionId);
  }

  public static Set<String> getDownloadedDatapacks(){
    return downloadedDatapacks.keySet();
  }

  public static Datapack getOrCreateDatapack(String datapackName) {
    Datapack datapack = getDatapack(datapackName);
    if (datapack != null) {
      return datapack;
    }

    datapack = new Datapack(datapackName);
    DatapackManager.addDatapack(datapackName, datapack);
    return datapack;
  }

  public static Path getRootPath() {
    return datapackRootPath;
  }

  public static MinecraftServer getServer() {
    return minecraftServer;
  }

  public static void unzipDatapack(Path zipFilePath, Path targetDatapackPath) throws IOException {
    Plunger.debug("Path: " + targetDatapackPath);
    if (targetDatapackPath.toFile().exists()) {
      // Clear the original datapack
      // TODO:BRENT offer to create a new version of it?
      ZipUtility.deleteDirectory(targetDatapackPath);
    }

    ZipUtility.unzipFile(zipFilePath, targetDatapackPath);
  }

  public static void placeDownloadedDatapackFiles(MinecraftServer server, String datapackIdString) {
    DownloadedPack downloadedPack = downloadedDatapacks.get(datapackIdString);
    String datapackName = downloadedPack.packIdentifier.getPath();

    // TODO: Brent, Check for name duplicates and Should prompt with Overwrite or new name

    try {
      unzipDatapack(downloadedPack.installFile, Paths.get(datapackRootPath.toString(), datapackName));
    } catch (IOException e) {
      Plunger.error("Failed to installDatapackFromZip", e);
    }
  }

  public static void onServerStarted(MinecraftServer server) {
    MinecraftServerAccessor serverAccess = (MinecraftServerAccessor)(server);
    Session session = serverAccess.getSession();

    // Create and Cache the StoragePath
    setStorageFolders(session);
    // Cache the datapackRootPath
    datapackRootPath = session.getDirectory(WorldSavePath.DATAPACKS);
    minecraftServer = server;

    // Load custom dimension files
    for(Datapack datapack : DatapackManager.datapackCache.values()) {
      datapack.setRootPath(datapackRootPath);
      datapack.loadQueuedDimensions();
    }

    // Build list of Custom Datapacks (datapacks in the datapack folder)
    // This allows us to create an empty datapack and then add things later
    Collection<String> enabledDataPacks = server.getDataPackManager().getEnabledNames();
    for (String datapackFullName : enabledDataPacks) {
      if (datapackFullName.startsWith("file/")) {
        String packName = datapackFullName.replaceAll("file/", "");

        // Creates the datapack or just gets it and is ignored
        getOrCreateDatapack(packName);
      }
    }
  }

  public static void registerDatapackDimension(String datapackName, Identifier dimensionId) {
    dimensionToDatapackMap.put(dimensionId, datapackName);
  }

  public static void setStorageFolders(Session session) {
    String minecraftFolder = session.getDirectory(WorldSavePath.ROOT).toString();

    String sandboxDirName = Paths.get(minecraftFolder, "sandbox").toString();
    File sandboxDirFile = new File(sandboxDirName);
    if (!sandboxDirFile.exists()) {
      sandboxDirFile.mkdir();
    }

    Path storageDirPath = Paths.get(sandboxDirName, "storage");
    File storageDirFile = new File(storageDirPath.toString());
    if (!storageDirFile.exists()) {
      storageDirFile.mkdir();
    }

    Path dimensionStoragePath = Paths.get(sandboxDirName, "dimensions");
    File dimensionStorageFile = new File(dimensionStoragePath.toString());
    if (!dimensionStorageFile.exists()) {
      dimensionStorageFile.mkdir();
    }

    dimensionStorageDirectory = dimensionStoragePath;
    storageDirectory = storageDirPath;
  }

  public static Path getDimensionStorageFolder() {
    return dimensionStorageDirectory;
  }

  // Using Session so this can be used outside commands
  public static Path getStorageFolder() {
    return storageDirectory;
  }
}
