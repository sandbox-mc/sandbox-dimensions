package io.sandbox.dimensions.dimension;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.sandbox.dimensions.Main;
import io.sandbox.dimensions.mixin.MinecraftServerAccessor;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.level.storage.LevelStorage.Session;

public class DimensionManager {
  private static List<String> initializedDimensions = new ArrayList<>();
  private static List<Identifier> sandboxDimensionWorldFiles = new ArrayList<>();
  private static Map<Identifier, DimensionSave> dimensionSaves = new HashMap<>();
  private static Path storageDirectory = null;

  public static void init() {
    ResourceManagerHelper.get(ResourceType.SERVER_DATA)
    .registerReloadListener(new SimpleSynchronousResourceReloadListener() {
      @Override
      public Identifier getFabricId() {
        return Main.id("world_save_loader");
      }

      @Override
      public void reload(ResourceManager manager) {
        Map<Identifier, Resource> dimensions = manager.findResources("dimension", path -> true);
        for (Identifier dimensionName : dimensions.keySet()) {
          System.out.println("Dimension: " + dimensionName);
          initializedDimensions.add(dimensionName.toString());
        }
        // test_realm:world_saves/my_world.zip
        // Load all template pools for reference later
        Map<Identifier, Resource> worldSaves = manager.findResources("world_saves", path -> true);
        for (Identifier resourceName : worldSaves.keySet()) {
          String dataPackName = resourceName.getNamespace();
          System.out.println("TEst: " + worldSaves.get(resourceName).getResourcePackName() + " : " + resourceName.getPath());

          // Resource resource = worldSaves.get(resourceName);
          // Pathing should match for Namespace and fileName
          String dimensionIdentifier = resourceName.toString()
            .replaceAll("world_saves", "dimension")
            .replaceAll(".zip", ".json");
          if (initializedDimensions.contains(dimensionIdentifier)) {
            System.out.println("Loaded: " + dataPackName + " : " + resourceName);
            if (!sandboxDimensionWorldFiles.contains(resourceName)) {
              sandboxDimensionWorldFiles.add(resourceName);
            }
          }
        }
      }
    });
  }

  public static Path getStorageFolder(ServerCommandSource source) {
    if (storageDirectory != null) {
      return storageDirectory;
    }

    Session session = ((MinecraftServerAccessor)source.getServer()).getSession();

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

  public static void processSandboxDimensionFiles(ServerWorld server) {
    var storage = server.getPersistentStateManager();
    for (Identifier dimensionId : sandboxDimensionWorldFiles) {
      DimensionSave dimensionSave = new DimensionSave();
      dimensionSaves.put(dimensionId, dimensionSave);
    }
  }
}
