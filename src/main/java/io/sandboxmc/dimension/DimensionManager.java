package io.sandboxmc.dimension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import io.sandboxmc.Main;
import io.sandboxmc.chunkGenerators.EmptyChunkGenerator;
import io.sandboxmc.datapacks.DatapackManager;
import io.sandboxmc.mixin.MinecraftServerAccessor;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry.Reference;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.RandomSequencesState;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorderListener.WorldBorderSyncer;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.level.UnmodifiableLevelProperties;
import net.minecraft.world.level.storage.LevelStorage.Session;

public class DimensionManager {
  private static Set<String> datapackFolderSet = new HashSet<String>();
  private static Map<String, Identifier> sandboxDefaultIdentifiers = new HashMap<>();
  private static List<String> initializedDimensions = new ArrayList<>();
  private static Map<String, String> sandboxDimensionWorldFiles = new HashMap<>();
  private static Map<String, DimensionSave> dimensionSaves = new HashMap<>();
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

  public static void addDimensionSave(String dimensionName, DimensionSave dimensionSave) {
    dimensionSaves.put(dimensionName, dimensionSave);
  }

  // This will generate a dimension with EmptyChunkGenerator
  public static void createDimensionWorld(MinecraftServer server, Identifier dimensionIdentifier, Identifier dimensionTypeId) {
    MinecraftServerAccessor serverAccess = (MinecraftServerAccessor)(server);
    Session session = serverAccess.getSession();
    RegistryKey<World> registryKey = RegistryKey.of(RegistryKeys.WORLD, dimensionIdentifier);
    SaveProperties saveProperties = server.getSaveProperties();
    UnmodifiableLevelProperties unmodifiableLevelProperties = new UnmodifiableLevelProperties(
      saveProperties,
      saveProperties.getMainWorldProperties()
    );

    Optional<Reference<DimensionType>> dimensionType = server.getRegistryManager().get(
      RegistryKeys.DIMENSION_TYPE
    ).getEntry(RegistryKey.of(RegistryKeys.DIMENSION_TYPE, dimensionTypeId));

    // Optional<Reference<DimensionType>> dimensionType = server.getRegistryManager().get(
    //   RegistryKeys.DIMENSION_TYPE
    // ).getEntry(DimensionTypes.OVERWORLD);

    if (!dimensionType.isPresent()) {
      System.out.println("Warning: Failed to create world with dimensionType: " + dimensionTypeId);
      return;
    }

    var chunkGen = new EmptyChunkGenerator(server.getRegistryManager().get(RegistryKeys.BIOME).getEntry(0).get());
    DimensionOptions test = new DimensionOptions(dimensionType.get(), chunkGen);

    ServerWorld dimensionWorld = new ServerWorld(
      server,
      serverAccess.getWorkerExecutor(),
      session,
      unmodifiableLevelProperties,
      registryKey,
      // dimensionOption,
      test,
      serverAccess.getWorldGenerationProgressListenerFactory().create(11),
      false,
      server.getWorld(World.OVERWORLD).getSeed(),
      ImmutableList.of(),
      true,
      (RandomSequencesState)null
    );
    server.getWorld(World.OVERWORLD).getWorldBorder().addListener(new WorldBorderSyncer(dimensionWorld.getWorldBorder()));
    serverAccess.getWorlds().put(registryKey, dimensionWorld);
    DimensionSave dimensionSave = DimensionSave.buildDimensionSave(dimensionWorld);
    int spawnX = unmodifiableLevelProperties.getSpawnX();
    int spawnY = chunkGen.getSeaLevel();
    int spawnZ = unmodifiableLevelProperties.getSpawnZ();
    BlockPos blockPos = new BlockPos(spawnX, spawnY, spawnZ);
    dimensionSave.setSpawnPos(dimensionWorld, blockPos);
    // DimensionManager.addDimensionSave(dimensionIdentifier.toString(), dimensionSave);
    
    BlockState blockState = Registries.BLOCK.get(new Identifier("grass_block")).getDefaultState();
    dimensionWorld.setBlockState(blockPos, blockState);
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
      worldMap.put(world.getRegistryKey().getValue().toString(), world);
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
        DimensionManager.addDimensionSave(dimensionIdString, dimensionSave);
      } else {
        System.out.println("WARNING: Failed to load world for: " + dimensionIdString);
      }
    }
  }
}
