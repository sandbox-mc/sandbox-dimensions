package io.sandboxmc.dimension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import io.sandboxmc.Plunger;
import io.sandboxmc.SandboxMC;
import io.sandboxmc.mixin.MinecraftServerAccessor;
import io.sandboxmc.zip.ZipUtility;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.block.BlockState;
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
import net.minecraft.world.level.storage.LevelStorage;

public class DimensionManager {
  private static Map<Identifier, DimensionSave> dimensionSaves = new HashMap<>();
  private static MinecraftServer minecraftServer = null;

  public static void addDimensionSave(Identifier dimensionName, DimensionSave dimensionSave) {
    dimensionSaves.put(dimensionName, dimensionSave);
  }

  public static void deleteDimension(Identifier dimensionIdentifier) {
    RegistryKey<World> dimensionKey = RegistryKey.of(RegistryKeys.WORLD, dimensionIdentifier);
    ServerWorld dimension = minecraftServer.getWorld(dimensionKey);
    MinecraftServerAccessor serverAccess = (MinecraftServerAccessor)(minecraftServer);

    ServerWorldEvents.UNLOAD.invoker().onWorldUnload(minecraftServer, dimension);
    if (serverAccess.getWorlds().remove(dimensionKey, dimension)) {
      dimensionSaves.get(dimensionIdentifier).deleteConfigFiles();
      dimensionSaves.remove(dimensionKey.getValue());

      // remove from generated list to prevent attenpts to create it on restart
      DimensionSave mainSave = DimensionManager.getOrCreateDimensionSave(minecraftServer.getWorld(World.OVERWORLD));
      mainSave.removeGeneratedWorld(dimensionIdentifier);

      LevelStorage.Session session = serverAccess.getSession();
      File worldDirectory = session.getWorldDirectory(dimensionKey).toFile();
      Plunger.info("Delete Dir: " + worldDirectory);
      if (worldDirectory.exists()) {
        try {
          ZipUtility.deleteDirectory(worldDirectory.toPath());
          Path namespaceDir = worldDirectory.toPath().getParent();
          if (namespaceDir.toFile().list().length == 0) {
            namespaceDir.toFile().delete();
          }
        } catch (IOException e) {
          Plunger.error("Failed to delete world directory", e);
        }
      }
    }
  }

  public static Set<Identifier> getDimensionList() {
    return dimensionSaves.keySet();
  }

  public static Map<Identifier, DimensionSave> getDimensions() {
    return dimensionSaves;
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

  public static void onServerStarted(MinecraftServer server) {
    minecraftServer = server;
  }

  public static void processSandboxDimensionFiles(MinecraftServer server) {
    // Set the minecraftServer for /reload command
    if (DimensionManager.minecraftServer == null) {
      DimensionManager.minecraftServer = server;
    }

    // Add sandbox_<GUID> datapack to load these

    // MinecraftServerAccessor serverAccess = (MinecraftServerAccessor)(server);
    // Session session = serverAccess.getSession();
    // String minecraftFolder = session.getDirectory(WorldSavePath.ROOT).toString();
    // Path standAloneDimensions = Paths.get(minecraftFolder, "sandbox", "dimensions", "data");
    // File dimensionsDataFolder = standAloneDimensions.toFile();
    // if (dimensionsDataFolder.exists()) {
    //   File[] namespaces = dimensionsDataFolder.listFiles((dir, name) -> dir.isDirectory());
    //   for (File namespaceFolder : namespaces) {
    //     Plunger.info("Namespace: " + namespaceFolder.toPath());
    //     // Check for sandbox_dimension files
    //     File sandboxDimensionFolder = Paths.get(
    //       namespaceFolder.toPath().toString(),
    //       DimensionSave.DIMENSION_CONFIG_FOLDER
    //     ).toFile();

    //     if (sandboxDimensionFolder.exists()) {

    //     }
    //   }
    // }

    // Create Dimensions that were never added to a datapack
    // TODO:BRENT add the config files to this to be tracked, rather than on main world
    // Need to get list of configs in the /storage folder or build a list of all dimensions
    // that are not part of a datapack
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
    dimensionSaves = new HashMap<>();
    minecraftServer = null;
  } 
}
