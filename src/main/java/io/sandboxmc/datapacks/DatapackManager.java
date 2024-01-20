package io.sandboxmc.datapacks;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import io.sandboxmc.datapacks.types.DownloadedPack;
import io.sandboxmc.dimension.DimensionManager;
import io.sandboxmc.dimension.zip.ZipUtility;
import io.sandboxmc.mixin.MinecraftServerAccessor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.level.storage.LevelStorage.Session;

public class DatapackManager {
  private static Path datapackPath;
  private static Map<String, Datapack> datapackCache = new HashMap<>();
  private static Map<String, String> dimensionToDatapackMap = new HashMap<>();
  private static Map<String, DownloadedPack> DownloadedDatapacks = new HashMap<>();

  public static void init(MinecraftServer server) {
    MinecraftServerAccessor serverAccess = (MinecraftServerAccessor)(server);
    Session session = serverAccess.getSession();
    // Cache the datapackPath for later use
    datapackPath = session.getDirectory(WorldSavePath.DATAPACKS);

    Path storageFolder = DimensionManager.getStorageFolder(session);
    DownloadedDatapacks.put(
      "product17:dog-house",
      new DownloadedPack(
        new Identifier("product17", "dog-house"),
        Paths.get(storageFolder.toString(), "product17", "dog-house.zip")
      )
    );
    // DownloadedDatapacks.put("dog-house", Paths.get(storageFolder.toString(), "product17", "dog-house.zip"));

    // Build list of files/dirs in /datapacks directory
    File[] folderList = datapackPath.toFile().listFiles((dir, name) -> dir.isDirectory());
    for (File file : folderList) {
      String datapackName = file.getName();
      createDatapack(datapackName);
    }
  }

  public static void addDownloadedDatapack(Identifier name, Path zipFile) {
    DownloadedDatapacks.put(name.toString(), new DownloadedPack(name, zipFile));
  }

  public static Datapack createDatapack(String datapackName) {
    Datapack datapack = new Datapack(datapackPath, datapackName);
    datapackCache.put(datapackName, datapack);
    return datapack;
  }

  public static Datapack getDatapack(String datapackName) {
    return datapackCache.get(datapackName);
  }

  public static String getDatapackName(Identifier dimensionId) {
    return dimensionToDatapackMap.get(dimensionId.toString());
  }

  public static Set<String> getDownloadedDatapacks(){
    return DownloadedDatapacks.keySet();
  }

  public static void installDatapackFromZip(Path zipFilePath, Path targetDatapackPath) throws IOException {
    System.out.println("Path: " + targetDatapackPath);
    if (targetDatapackPath.toFile().exists()) {
      // Clear the original datapack
      // TODO: offer to create a new version of it?
      ZipUtility.deleteDirectory(targetDatapackPath);
    }

    ZipUtility.unzipFile(zipFilePath, targetDatapackPath);
  }

  public static void installDownloadedDatapack(MinecraftServer server, String datapackIdString) {
    DownloadedPack downloadedPack = DownloadedDatapacks.get(datapackIdString);
    String datapackName = downloadedPack.packIdentifier.getPath();

    try {
      installDatapackFromZip(downloadedPack.installFile, Paths.get(datapackPath.toString(), datapackName));
    } catch (IOException e) {
      // TODO Auto-generated catch block
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
}
