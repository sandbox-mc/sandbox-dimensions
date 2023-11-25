package io.sandboxmc.datapacks;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import io.sandboxmc.mixin.MinecraftServerAccessor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.level.storage.LevelStorage.Session;

public class DatapackManager {
  private static Path datapackPath;
  private static Map<String, Datapack> datapackCache = new HashMap<>();

  public static void init(MinecraftServer server) {
    MinecraftServerAccessor serverAccess = (MinecraftServerAccessor)(server);
    Session session = serverAccess.getSession();
    // Cache the datapackPath for later use
    datapackPath = session.getDirectory(WorldSavePath.DATAPACKS);

    // Build list of files/dirs in /datapacks directory
    File[] folderList = datapackPath.toFile().listFiles((dir, name) -> dir.isDirectory());
    for (File file : folderList) {
      String datapackName = file.getName();
      Datapack datapack = new Datapack(datapackPath, datapackName);
      datapackCache.put(datapackName, datapack);
    }
  }

  public static Datapack createDatapack(String datapackName) {
    Datapack datapack = new Datapack(datapackPath, datapackName);
    datapackCache.put(datapackName, datapack);
    return datapack;
  }

  public static Datapack geDatapack(String datapackName) {
    return datapackCache.get(datapackName);
  }
}
