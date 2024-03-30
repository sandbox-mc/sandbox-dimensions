package io.sandboxmc.dimension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import io.sandboxmc.Plunger;
import io.sandboxmc.SandboxMC;
import io.sandboxmc.datapacks.DatapackManager;
import io.sandboxmc.mixin.MinecraftServerAccessor;
import io.sandboxmc.zip.ZipUtility;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.block.BlockState;
import net.minecraft.registry.DynamicRegistryManager.Immutable;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;
import net.minecraft.world.PersistentState.Type;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.level.storage.LevelStorage;

public class DimensionManager {
  private static Set<String> datapackFolderSet = new HashSet<String>();
  private static Map<Identifier, String> sandboxDimensionWorldFiles = new HashMap<>();
  private static Map<Identifier, DimensionSave> dimensionSaves = new HashMap<>();
  private static MinecraftServer minecraftServer = null;

  public static void init() {
    // ResourceManagerHelper.get(ResourceType.SERVER_DATA)
    // .registerReloadListener(new SimpleSynchronousResourceReloadListener() {
    //   @Override
    //   public Identifier getFabricId() {
    //     return Main.id("world_save_loader");
    //   }

    //   @Override
    //   public void reload(ResourceManager manager) {
    //     List<String> initializedDimensions = new ArrayList<>();

    //     // Build a list of Dimensions coming from Datapacks
    //     Map<Identifier, Resource> dimensions = manager.findResources("dimension", path -> true);
    //     for (Identifier dimensionName : dimensions.keySet()) {
    //       String dimPath = dimensionName.getPath()
    //         .replaceAll("dimension/", "")
    //         .replaceAll(".json", "");
    //       String dimensionRegistrationKey = dimensionName.getNamespace() + ":" + dimPath;
    //       System.out.println("DimensionKey: " + dimensionRegistrationKey);
    //       initializedDimensions.add(dimensionRegistrationKey);
    //     }

    //     // Build a list of World Save files from Datapacks
    //     Map<Identifier, Resource> worldSaves = manager.findResources(DimensionSave.WORLD_SAVE_FOLDER, path -> true);
    //     for (Identifier resourceName : worldSaves.keySet()) {
    //       Resource resource = worldSaves.get(resourceName);
    //       String packName = resource.getResourcePackName().replaceAll("file/", "");
    //       System.out.println("Pathing: " + resourceName);
    //       // Pathing should match for Namespace and fileName
    //       String dimensionKey = resourceName.toString()
    //         .replaceAll(DimensionSave.WORLD_SAVE_FOLDER + "/", "") // remove folder name
    //         .replaceAll(".zip", "");
    //       Identifier dimensioIdentifier = new Identifier(dimensionKey);
    //       // Check if there is an initialized dimension for the save file
    //       if (initializedDimensions.contains(dimensionKey)) {
    //         if (!sandboxDimensionWorldFiles.containsKey(dimensioIdentifier)) {
    //           sandboxDimensionWorldFiles.put(dimensioIdentifier, packName);
    //         }
    //       } else {
    //         System.out.println("WARNING: " + resourceName + " does not have a dimension loaded from a Datapack");
    //       }
    //     }

    //     if (DimensionManager.minecraftServer != null) {
    //       DimensionManager.processSandboxDimensionFiles(DimensionManager.minecraftServer);
    //     }
    //   }
    // });
  }

  // public static void addDimensionToPacknameMap(Identifier dimensionIdentifier, String packName) {
  //   sandboxDimensionWorldFiles.put(dimensionIdentifier, packName);
  // }

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
    SandboxWorldConfig config = new SandboxWorldConfig(server);
    config.setSeed(seed);
    if (dimensionOptions != null) {
      config.setDimensionOptions(dimensionOptions);
    } else {
      Plunger.error("Warning: Failed to create world with dimensionOptions: " + dimensionOptionsId);
      return;
    }

    // Create the World
    SandboxWorld dimensionWorld = new SandboxWorld(server, registryKey, config);
    // TODO:BRENT check if we need/want to align the borders, I don't know if we do...
    // server.getWorld(World.OVERWORLD).getWorldBorder().addListener(new WorldBorderSyncer(dimensionWorld.getWorldBorder()));
    serverAccess.getWorlds().put(registryKey, dimensionWorld);
    DimensionSave dimensionSave = DimensionManager.getOrCreateDimensionSave(dimensionWorld, true);

    // Add dimension to generated list so it can be restored on server restart
    DimensionSave mainSave = DimensionManager.getOrCreateDimensionSave(server.getWorld(World.OVERWORLD));
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
      dimensionSaves.get(dimensionIdentifier).deleteConfigFiles();
      dimensionSaves.remove(dimensionKey.getValue());
      
      // remove from generated list to prevent attenpts to create it on restart
      DimensionSave mainSave = DimensionManager.getOrCreateDimensionSave(server.getWorld(World.OVERWORLD));
      mainSave.removeGeneratedWorld(dimensionIdentifier);

      LevelStorage.Session session = serverAccess.getSession();
      File worldDirectory = session.getWorldDirectory(dimensionKey).toFile();
      Plunger.debug("Delete Dir: " + worldDirectory);
      if (worldDirectory.exists()) {
        try {
          ZipUtility.deleteDirectory(worldDirectory.toPath());
          Path namespaceDir = worldDirectory.toPath().getParent();
          if (namespaceDir.toFile().list().length == 0) {
            namespaceDir.toFile().delete();
          }
        } catch (IOException e) {
          Plunger.error("Failed to delete world directory", e);
          try {
            ZipUtility.deleteDirectory(worldDirectory.toPath());
          } catch (IOException ignored) {
          }
        }
      }
    }
  }

  public static Set<Identifier> getDimensionList() {
    return dimensionSaves.keySet();
  }

  public static DimensionSave getDimensionSave(Identifier dimensionName) {
    return dimensionSaves.get(dimensionName);
  }

  public static DimensionSave buildDimensionSaveFromConfig(Identifier dimensionIdentifier, SandboxWorldConfig sandboxConfig) {
    if (minecraftServer == null) {
      return null;
    }

    MinecraftServerAccessor serverAccess = (MinecraftServerAccessor)(minecraftServer);
    RegistryKey<World> registryKey = RegistryKey.of(RegistryKeys.WORLD, dimensionIdentifier);
    Identifier dimensionOptionsId = sandboxConfig.getDimensionOptionsId();
    if (dimensionOptionsId == null) {
      // TODO: Brent, this throws... I think, sometimes...
      dimensionOptionsId = sandboxConfig.getDimensionOptions().dimensionTypeEntry().getKey().get().getValue();
    }
    Boolean isEmptyWorld = dimensionOptionsId.equals(SandboxMC.id("empty"));
  
    // Create the World
    SandboxWorld dimensionWorld = new SandboxWorld(minecraftServer, registryKey, sandboxConfig);

    // TODO:BRENT check if we need/want to align the borders, I don't know if we do...
    // server.getWorld(World.OVERWORLD).getWorldBorder().addListener(new WorldBorderSyncer(dimensionWorld.getWorldBorder()));
    serverAccess.getWorlds().put(registryKey, dimensionWorld);
    DimensionSave dimensionSave = DimensionManager.getOrCreateDimensionSave(dimensionWorld, true);
    
    // Set saveConfig
    dimensionSave.loadDimensionFile();

    // Add dimension to generated list so it can be restored on server restart
    DimensionSave mainSave = DimensionManager.getOrCreateDimensionSave(minecraftServer.getWorld(World.OVERWORLD));
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

    return dimensionSave;
  }

  // public static String getPackFolder(Identifier dimensionId) {
  //   return sandboxDimensionWorldFiles.get(dimensionId);
  // }

  private static Type<DimensionSave> type = new Type<>(
    DimensionSave::new, // If there's no 'DimensionSave' yet create one
    DimensionSave::createFromNbt, // If there is a 'DimensionSave' NBT, parse it with 'createFromNbt'
    null // Supposed to be an 'DataFixTypes' enum, but we can just pass null
  );

  public static DimensionSave getOrCreateDimensionSave(ServerWorld dimension) {
    return getOrCreateDimensionSave(dimension, false);
  }

  // setActive is for initializing worlds as active in sandbox stuff
  // This is defaulted to false for default worlds such as Overworld for now
  // It is also used to check if a world has been fully loaded or not from a save file
  public static DimensionSave getOrCreateDimensionSave(ServerWorld dimension, Boolean setActive) {
    dimension.getServer();
    Identifier dimensionId = dimension.getRegistryKey().getValue();
    DimensionSave dimensionSave = DimensionManager.getDimensionSave(dimensionId);
    if (dimensionSave != null) {
      return dimensionSave;
    }

    PersistentStateManager persistentStateManager = dimension.getPersistentStateManager();
    dimensionSave = persistentStateManager.getOrCreate(type, SandboxMC.MOD_ID);
    dimensionSave.setIdentifer(dimensionId);
    dimensionSave.setServerWorld(dimension);
    if (dimensionSave.dimensionIsActive || setActive) {
      dimensionSave.dimensionIsActive = true;
      DimensionManager.addDimensionSave(dimensionId, dimensionSave);
    }

    dimensionSave.markDirty();
    return dimensionSave;
  }

  public static void onServerStart(MinecraftServer server) {
    minecraftServer = server;
  }

  public static void processSandboxDimensionFiles(MinecraftServer server) {
    // Set the minecraftServer for /reload command
    if (DimensionManager.minecraftServer == null) {
      DimensionManager.minecraftServer = server;
    }

    // Map<Identifier, ServerWorld> worldMap = new HashMap<>();

    // // Get a mapping of the currently loaded worlds for persistent storage
    // for (ServerWorld world : server.getWorlds()) {
    //   Identifier worldId = world.getRegistryKey().getValue();
    //   if (!sandboxDimensionWorldFiles.containsKey(worldId)) {
    //     DimensionManager.getOrCreateDimensionSave(world);
    //   }

    //   // DimensionConfig.create(world);

    //   worldMap.put(worldId, world);
    // }

    // // Loop through all the saves that are found
    // for (Identifier dimensionId : sandboxDimensionWorldFiles.keySet()) {
    //   ServerWorld dimensionWorld = worldMap.get(dimensionId);
    //   if (dimensionWorld != null) {
    //     // Register dimension to datapack
    //     Plunger.debug("Register: " + dimensionId + " : " + sandboxDimensionWorldFiles.get(dimensionId));
    //     DatapackManager.registerDatapackDimension(sandboxDimensionWorldFiles.get(dimensionId), dimensionId);
    //     DimensionSave dimensionSave = DimensionManager.getOrCreateDimensionSave(dimensionWorld);
    //     if (!dimensionSave.dimensionSaveLoaded) {
    //       Plunger.debug("Loading World File: " + dimensionId);
    //       // Load datapack save zip and set as loaded
    //       Boolean loaded = dimensionSave.loadDimensionFile();
    //       if (!loaded) {
    //         Plunger.debug("WARNING: Failed to load world for: " + dimensionId);
    //       }
    //     } else {
    //       Plunger.debug("World Save files already loaded, Skipping: " + dimensionId);
    //     }

    //     // Add to list for auto-complete
    //     DimensionManager.addDimensionSave(dimensionId, dimensionSave);
    //   } else {
    //     Plunger.debug("WARNING: Failed to load world for: " + dimensionId);
    //   }
    // }

    // Create Dimensions that were never added to a datapack
    // TODO: Brent, add the config files to this to be tracked, rather than on main world
    HashMap<Identifier, Identifier> mainSave = DimensionManager
      .getOrCreateDimensionSave(server.getWorld(World.OVERWORLD))
      .getGeneratedWorlds();

    // mainSave.keySet().forEach(dimensionId -> {
    //   if (DimensionManager.getDimensionSave(dimensionId) == null) {
    //     Plunger.debug("WARNING: Failed to load world for: ");
    //     // Create the dimension
    //     SandboxWorldConfig config = new SandboxWorldConfig();
    //     // Longs default to 0, so null arg would be 0, not sure what we should do if some passes 0...
    //     config.setSeed(server.getWorld(World.OVERWORLD).getSeed());
    //     config.setDimensionOptionsId(mainSave.get(dimensionId));
    //     DimensionManager.buildDimensionSaveFromConfig(dimensionId, config);
    //   }
    // });
  }

  public static void unload() {
    datapackFolderSet = new HashSet<String>();
    sandboxDimensionWorldFiles = new HashMap<>();
    dimensionSaves = new HashMap<>();
    minecraftServer = null;
  } 
}
