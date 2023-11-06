package io.sandbox.dimensions.dimension;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.sandbox.dimensions.Main;
import io.sandbox.dimensions.mixin.MinecraftServerAccessor;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.level.storage.LevelStorage.Session;

public class DimensionManager {
  private static Map<String, Identifier> sandboxDefaultIdentifiers = new HashMap<>();
  private static List<String> initializedDimensions = new ArrayList<>();
  private static Map<String, String> sandboxDimensionWorldFiles = new HashMap<>();
  private static Map<String, DimensionSave> dimensionSaves = new HashMap<>();
  private static Path storageDirectory = null;
  private static MinecraftServer minecraftServer = null;

  public static void init() {
    ResourceManagerHelper.get(ResourceType.SERVER_DATA)
    .registerReloadListener(new SimpleSynchronousResourceReloadListener() {
      @Override
      public Identifier getFabricId() {
        return Main.id("world_save_loader");
      }

      @Override
      public void reload(ResourceManager manager) {
        // Use the Identifier in the server ResourceManager to get the inputStream to copy
        Map<Identifier, Resource> defaults = manager.findResources("sandbox-defaults", path -> true);
        for (Identifier defaultIdentifier : defaults.keySet()) {
          String defaultType = defaultIdentifier.getPath()
            .replaceAll("sandbox-defaults/", "")
            .replaceAll(".json", "") // Remove file type .zip and .json
            .replaceAll(".zip", "");
          sandboxDefaultIdentifiers.put(defaultType, defaultIdentifier);
        }

        Map<Identifier, Resource> dimensions = manager.findResources("dimension", path -> true);
        for (Identifier dimensionName : dimensions.keySet()) {
          String dimPath = dimensionName.getPath()
            .replaceAll("dimension/", "")
            .replaceAll(".json", "");
          String dimensionRegistrationKey = dimensionName.getNamespace() + ":" + dimPath;
          System.out.println("DimensionKey: " + dimensionRegistrationKey);
          initializedDimensions.add(dimensionRegistrationKey);
        }

        // test_realm:world_saves/my_world.zip
        // Load all template pools for reference later
        Map<Identifier, Resource> worldSaves = manager.findResources(DimensionSave.WORLD_SAVE_FOLDER, path -> true);
        for (Identifier resourceName : worldSaves.keySet()) {
          Resource resource = worldSaves.get(resourceName);
          String packName = resource.getResourcePackName().replaceAll("file/", "");
          System.out.println("Pathing: " + resourceName);
          // Pathing should match for Namespace and fileName
          String dimensionKey = resourceName.toString()
            .replaceAll(DimensionSave.WORLD_SAVE_FOLDER + "/", "") // remove folder name
            .replaceAll(".zip", "");
          // Check if there is an initialized dimension for the save file
          if (initializedDimensions.contains(dimensionKey)) {
            if (!sandboxDimensionWorldFiles.containsKey(dimensionKey)) {
              sandboxDimensionWorldFiles.put(dimensionKey, packName);
            }
          } else {
            System.out.println("WARNING: " + resourceName + " does not have a dimension");
          }
        }

        if (DimensionManager.minecraftServer != null) {
          DimensionManager.processSandboxDimensionFiles(DimensionManager.minecraftServer);
        }
      }
    });
  }

  public static void addDimensionPackName(String dimensionKey, String packName) {
    sandboxDimensionWorldFiles.put(dimensionKey, packName);
  }

  public static void addDimensionSave(String dimensionName, DimensionSave dimensionSave) {
    dimensionSaves.put(dimensionName, dimensionSave);
  }

  public static Identifier getDefaultConfig(String defaultType) {
    return sandboxDefaultIdentifiers.get(defaultType);
  }

  public static Set<String> getDimensionList() {
    return dimensionSaves.keySet();
  }

  public static String getPackFolder(String dimensionId) {
    return sandboxDimensionWorldFiles.get(dimensionId);
  }

  public static HashSet<String> getPackFolders() {
    return new HashSet<String>(sandboxDimensionWorldFiles.values());
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

  public static void processSandboxDimensionFiles(MinecraftServer server) {
    // Set the minecraftServer for /reload command
    if (DimensionManager.minecraftServer == null) {
      DimensionManager.minecraftServer = server;
    }

    Map<String, ServerWorld> worldMap = new HashMap<>();

    // Get a mapping of the currently loaded worlds for persistent storage
    for (ServerWorld world : server.getWorlds()) {
      worldMap.put(world.getRegistryKey().getValue().toString(), world);
    }

    // Loop through all the saves that are found
    for (String dimensionIdString : sandboxDimensionWorldFiles.keySet()) {
      ServerWorld dimensionWorld = worldMap.get(dimensionIdString);
      if (dimensionWorld != null) {
        DimensionSave dimensionSave = DimensionSave.getDimensionState(dimensionWorld);
        if (!dimensionSave.dimensionSaveLoaded) {
          System.out.println("Loading World File: " + dimensionIdString);
          // Load datapack save zip and set as loaded
          dimensionSave.dimensionSaveLoaded = DimensionSave.loadDimensionFile(dimensionIdString, server);
        } else {
          System.out.println("World Save files already loaded, Skipping: " + dimensionIdString);
        }

        // Add to list for auto-complete
        DimensionManager.addDimensionSave(dimensionIdString, dimensionSave);
      } else {
        System.out.println("WARNING: Failed to load world for: " + dimensionIdString);
      }
    }
  }
}