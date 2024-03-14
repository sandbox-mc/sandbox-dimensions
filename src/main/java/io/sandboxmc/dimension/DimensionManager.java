package io.sandboxmc.dimension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.sandboxmc.SandboxMC;
import io.sandboxmc.chunkGenerators.EmptyChunkGenerator;
import io.sandboxmc.datapacks.DatapackManager;
import io.sandboxmc.mixin.MinecraftServerAccessor;
import io.sandboxmc.zip.ZipUtility;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.block.BlockState;
import net.minecraft.registry.DynamicRegistryManager.Immutable;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.level.storage.LevelStorage;

public class DimensionManager {
  private static Set<String> datapackFolderSet = new HashSet<String>();
  private static Map<String, Identifier> sandboxDefaultIdentifiers = new HashMap<>();
  private static List<String> initializedDimensions = new ArrayList<>();
  private static Map<String, String> sandboxDimensionWorldFiles = new HashMap<>();
  private static Map<Identifier, DimensionSave> dimensionSaves = new HashMap<>();
  private static MinecraftServer minecraftServer = null;

  public static void init() {
    ResourceManagerHelper.get(ResourceType.SERVER_DATA)
    .registerReloadListener(new SimpleSynchronousResourceReloadListener() {
      @Override
      public Identifier getFabricId() {
        return SandboxMC.id("world_save_loader");
      }

      @Override
      public void reload(ResourceManager manager) {
        // Build a list of default zip data files for creating Datapacks later
        Map<Identifier, Resource> defaults = manager.findResources("sandbox-defaults", path -> true);
        for (Identifier defaultIdentifier : defaults.keySet()) {
          String defaultType = defaultIdentifier.getPath()
            .replaceAll("sandbox-defaults/", "")
            .replaceAll(".json", "") // Remove file type .zip and .json
            .replaceAll(".zip", "");
          sandboxDefaultIdentifiers.put(defaultType, defaultIdentifier);
        }

        // Build a list of Dimensions coming from Datapacks
        Map<Identifier, Resource> dimensions = manager.findResources("dimension", path -> true);
        for (Identifier dimensionName : dimensions.keySet()) {
          String dimPath = dimensionName.getPath()
            .replaceAll("dimension/", "")
            .replaceAll(".json", "");
          String dimensionRegistrationKey = dimensionName.getNamespace() + ":" + dimPath;
          System.out.println("DimensionKey: " + dimensionRegistrationKey);
          initializedDimensions.add(dimensionRegistrationKey);
        }

        // Build a list of World Save files from Datapacks
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
            System.out.println("WARNING: " + resourceName + " does not have a dimension loaded from a Datapack");
          }
        }

        if (DimensionManager.minecraftServer != null) {
          DimensionManager.processSandboxDimensionFiles(DimensionManager.minecraftServer);
        }
      }
    });
  }

  public static void addDimensionToPacknameMap(String dimensionKey, String packName) {
    sandboxDimensionWorldFiles.put(dimensionKey, packName);
  }

  public static void addDimensionSave(Identifier dimensionName, DimensionSave dimensionSave) {
    dimensionSaves.put(dimensionName, dimensionSave);
  }

  // This will generate a dimension with EmptyChunkGenerator
  public static void createDimensionWorld(MinecraftServer server, Identifier dimensionIdentifier, Identifier dimensionOptionsId, long seed) {
    MinecraftServerAccessor serverAccess = (MinecraftServerAccessor)(server);
    RegistryKey<World> registryKey = RegistryKey.of(RegistryKeys.WORLD, dimensionIdentifier);
    Immutable registryManager = server.getRegistryManager();

    // Get the presets for DimensionOptions
    // This should allow for custom dimension types to be generated
    DimensionOptions dimensionOptions = registryManager.get(RegistryKeys.DIMENSION).get(dimensionOptionsId);
    Boolean isEmptyWorld = dimensionOptionsId.equals(SandboxMC.id("empty"));
    SandboxWorldConfig config = new SandboxWorldConfig();
    config.setSeed(seed);
    if (isEmptyWorld) {
      // our empty world requires the custom chunkGenerator to prevent blocks from being placed
      var plainsBiome = registryManager.get(RegistryKeys.BIOME).getEntry(RegistryKey.of(RegistryKeys.BIOME, new Identifier("plains"))).get();
      var chunkGen = new EmptyChunkGenerator(plainsBiome);
      config.setGenerator(chunkGen);
    } else if (dimensionOptions != null) {
      config.setDimensionOptions(dimensionOptions);
    } else {
      System.out.println("Warning: Failed to create world with dimensionOptions: " + dimensionOptionsId);
      return;
    }

    // Create the World
    SandboxWorld dimensionWorld = new SandboxWorld(server, registryKey, config, isEmptyWorld);
    // TODO:BRENT check if we need/want to align the borders, I don't know if we do...
    // server.getWorld(World.OVERWORLD).getWorldBorder().addListener(new WorldBorderSyncer(dimensionWorld.getWorldBorder()));
    serverAccess.getWorlds().put(registryKey, dimensionWorld);
    DimensionSave dimensionSave = DimensionSave.buildDimensionSave(dimensionWorld, true);

    // Add dimension to generated list so it can be restored on server restart
    DimensionSave mainSave = DimensionSave.buildDimensionSave(server.getWorld(World.OVERWORLD));
    mainSave.addGeneratedWorld(dimensionIdentifier, dimensionOptionsId);

    // Set spawn point for single block placement
    int spawnX = dimensionWorld.getLevelProperties().getSpawnX();
    // TODO:BRENT adjust for generated terrain
    int spawnY = dimensionWorld.getSeaLevel();
    int spawnZ = dimensionWorld.getLevelProperties().getSpawnZ();
    BlockPos blockPos = new BlockPos(spawnX, spawnY, spawnZ);
    dimensionSave.setSpawnPos(dimensionWorld, blockPos);

    if (isEmptyWorld) {
      BlockState blockState = Registries.BLOCK.get(new Identifier("grass_block")).getDefaultState();
      dimensionWorld.setBlockState(blockPos, blockState);
    }
  }

  public static void deleteDimension(MinecraftServer server, Identifier dimensionIdentifier) {
    RegistryKey<World> dimensionKey = RegistryKey.of(RegistryKeys.WORLD, dimensionIdentifier);
    ServerWorld dimension = server.getWorld(dimensionKey);
    MinecraftServerAccessor serverAccess = (MinecraftServerAccessor)(server);

    if (serverAccess.getWorlds().remove(dimensionKey, dimension)) {
      ServerWorldEvents.UNLOAD.invoker().onWorldUnload(server, dimension);
      dimensionSaves.remove(dimensionKey.getValue());
      DimensionSave mainSave = DimensionSave.buildDimensionSave(server.getWorld(World.OVERWORLD));

      // remove from generated list to prevent attenpts to create it on restart
      mainSave.removeGeneratedWorld(dimensionIdentifier);

      LevelStorage.Session session = serverAccess.getSession();
      File worldDirectory = session.getWorldDirectory(dimensionKey).toFile();
      System.out.println("Delete Dir: " + worldDirectory);
      if (worldDirectory.exists()) {
        try {
          ZipUtility.deleteDirectory(worldDirectory.toPath());
          Path namespaceDir = worldDirectory.toPath().getParent();
          if (namespaceDir.toFile().list().length == 0) {
            namespaceDir.toFile().delete();
          }
        } catch (IOException e) {
          System.out.println("Failed to delete world directory");
          try {
            ZipUtility.deleteDirectory(worldDirectory.toPath());
          } catch (IOException ignored) {
          }
        }
      }
    }
  }

  public static Identifier getDefaultConfig(String defaultType) {
    return sandboxDefaultIdentifiers.get(defaultType);
  }

  public static Set<Identifier> getDimensionList() {
    return dimensionSaves.keySet();
  }

  public static DimensionSave getDimensionSave(Identifier dimensionName) {
    return dimensionSaves.get(dimensionName);
  }

  public static String getPackFolder(String dimensionId) {
    return sandboxDimensionWorldFiles.get(dimensionId);
  }

  public static HashSet<String> getPackFolders() {
    return new HashSet<String>(sandboxDimensionWorldFiles.values());
  }

  public static Set<String> getDatapackNames(Path datapackPath, Boolean folderOnly) throws IOException {
    // Only adding folders for now, don't want to deal with unzipping and zipping again
    File[] folderList = datapackPath.toFile().listFiles((dir, name) -> folderOnly ? dir.isDirectory() : true);
    for (File file : folderList) {
      String fileName = file.getName();
      if(!datapackFolderSet.contains(fileName)) {
        datapackFolderSet.add(fileName);
      }
    }

    return datapackFolderSet;
  }

  public static void processSandboxDimensionFiles(MinecraftServer server) {
    // Set the minecraftServer for /reload command
    if (DimensionManager.minecraftServer == null) {
      DimensionManager.minecraftServer = server;
    }

    Map<String, ServerWorld> worldMap = new HashMap<>();

    // Get a mapping of the currently loaded worlds for persistent storage
    for (ServerWorld world : server.getWorlds()) {
      String worldId = world.getRegistryKey().getValue().toString();
      if (!sandboxDimensionWorldFiles.containsKey(worldId)) {
        DimensionSave.buildDimensionSave(world);
      }

      worldMap.put(worldId, world);
    }

    // Loop through all the saves that are found
    for (String dimensionIdString : sandboxDimensionWorldFiles.keySet()) {
      Identifier dimensionId = new Identifier(dimensionIdString);
      ServerWorld dimensionWorld = worldMap.get(dimensionIdString);
      if (dimensionWorld != null) {
        // Register dimension to datapack
        System.out.println("Register: " + dimensionId + " : " + sandboxDimensionWorldFiles.get(dimensionIdString));
        DatapackManager.registerDatapackDimension(sandboxDimensionWorldFiles.get(dimensionIdString), dimensionId);
        DimensionSave dimensionSave = DimensionSave.buildDimensionSave(dimensionWorld);
        if (!dimensionSave.dimensionSaveLoaded) {
          System.out.println("Loading World File: " + dimensionIdString);
          // Load datapack save zip and set as loaded
          dimensionSave.dimensionSaveLoaded = DimensionSave.loadDimensionFile(dimensionId, server);
        } else {
          System.out.println("World Save files already loaded, Skipping: " + dimensionIdString);
        }

        // Add to list for auto-complete
        DimensionManager.addDimensionSave(dimensionId, dimensionSave);
      } else {
        System.out.println("WARNING: Failed to load world for: " + dimensionIdString);
      }
    }

    HashMap<Identifier, Identifier> mainSave = DimensionSave.buildDimensionSave(
      server.getWorld(World.OVERWORLD)
      ).getGeneratedWorlds();
      mainSave.keySet().forEach(dimensionId -> {
        if (DimensionManager.getDimensionSave(dimensionId) == null) {
        System.out.println("WARNING: Failed to load world for: ");
        DimensionManager.createDimensionWorld(
          server,
          dimensionId,
          mainSave.get(dimensionId),
          server.getWorld(World.OVERWORLD).getSeed()
        );
      }
    });
  }

  public static void unload() {
    datapackFolderSet = new HashSet<String>();
    sandboxDefaultIdentifiers = new HashMap<>();
    initializedDimensions = new ArrayList<>();
    sandboxDimensionWorldFiles = new HashMap<>();
    dimensionSaves = new HashMap<>();
    minecraftServer = null;
  } 
}
