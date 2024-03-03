package io.sandboxmc.datapacks;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import io.sandboxmc.datapacks.types.DownloadedPack;
import io.sandboxmc.mixin.MinecraftServerAccessor;
import io.sandboxmc.zip.ZipUtility;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.level.storage.LevelStorage.Session;

public class DatapackManager {
  private static Path datapackRootPath;
  private static Map<String, Datapack> datapackCache = new HashMap<>();
  private static Map<String, String> dimensionToDatapackMap = new HashMap<>();
  private static Map<String, DownloadedPack> downloadedDatapacks = new HashMap<>();
  private static Path storageDirectory = null;

  public static void init(MinecraftServer server) {
    MinecraftServerAccessor serverAccess = (MinecraftServerAccessor)(server);
    Session session = serverAccess.getSession();
    // Cache the datapackRootPath for later use
    datapackRootPath = session.getDirectory(WorldSavePath.DATAPACKS);

    // Path storageFolder = getStorageFolder(session);
    // downloadedDatapacks.put(
    //   "product17:dog-house",
    //   new DownloadedPack(
    //     new Identifier("product17", "dog-house"),
    //     Paths.get(storageFolder.toString(), "product17", "dog-house.zip")
    //   )
    // );
    // DownloadedDatapacks.put("dog-house", Paths.get(storageFolder.toString(), "product17", "dog-house.zip"));

    // Build list of files/dirs in /datapacks directory
    File[] folderList = datapackRootPath.toFile().listFiles((dir, name) -> dir.isDirectory());
    for (File file : folderList) {
      String datapackName = file.getName();
      createDatapack(datapackName);
    }
  }

  public static void addDatapack(String datapackName, Datapack datapack) {
    datapackCache.put(datapackName, datapack);
  }

  public static void addDownloadedDatapack(Identifier name, Path zipFile) {
    downloadedDatapacks.put(name.toString(), new DownloadedPack(name, zipFile));
  }

  public static Datapack createDatapack(String datapackName) {
    Datapack datapack = getDatapack(datapackName);
    if (datapack != null) {
      return datapack;
    }

    datapack = new Datapack(datapackRootPath, datapackName);
    DatapackManager.addDatapack(datapackName, datapack);
    return datapack;
  }

  public static Datapack getDatapack(String datapackName) {
    return datapackCache.get(datapackName);
  }

  public static String getDatapackName(Identifier dimensionId) {
    return dimensionToDatapackMap.get(dimensionId.toString());
  }

  public static Set<String> getDownloadedDatapacks(){
    return downloadedDatapacks.keySet();
  }

  public static void installDatapackFromZip(Path zipFilePath, Path targetDatapackPath) throws IOException {
    System.out.println("Path: " + targetDatapackPath);
    if (targetDatapackPath.toFile().exists()) {
      // Clear the original datapack
      // TODO:BRENT offer to create a new version of it?
      ZipUtility.deleteDirectory(targetDatapackPath);
    }

    ZipUtility.unzipFile(zipFilePath, targetDatapackPath);
  }

  public static void installDownloadedDatapack(MinecraftServer server, String datapackIdString) {
    DownloadedPack downloadedPack = downloadedDatapacks.get(datapackIdString);
    String datapackName = downloadedPack.packIdentifier.getPath();

    try {
      installDatapackFromZip(downloadedPack.installFile, Paths.get(datapackRootPath.toString(), datapackName));
    } catch (IOException e) {
      // TODO:BRENT Auto-generated catch block
      System.out.println("Failed");
      e.printStackTrace();
    }

    // Files are unzipped, now we can register the datapack
    Datapack datapack = getDatapack(datapackName);
    if (datapack == null) {
      datapack = createDatapack(datapackName);
    }

    datapack.initializeDatapack(server);
  }

  public static void registerDatapackDimension(String datapackName, Identifier dimensionId) {
    dimensionToDatapackMap.put(dimensionId.toString(), datapackName);
  }

  // Using Session so this can be used outside commands
  public static Path getStorageFolder(Session session) {
    if (storageDirectory != null) {
      return storageDirectory;
    }

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

    storageDirectory = storageDirPath;

    return storageDirPath;
  }
}
