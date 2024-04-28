package io.sandboxmc.dimension;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;

import io.sandboxmc.Plunger;
import io.sandboxmc.SandboxMC;
import io.sandboxmc.dimension.configs.DatapackDimensionConfig;
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
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.World;
import net.minecraft.world.PersistentState.Type;
import net.minecraft.world.level.storage.LevelStorage;
import net.minecraft.world.level.storage.LevelStorage.Session;

public class DimensionManager {
  private static Map<Identifier, DimensionSave> dimensionSaves = new HashMap<>();
  private static Gson gson = new Gson();
  private static MinecraftServer minecraftServer = null;

  public static void addDimensionSave(Identifier dimensionName, DimensionSave dimensionSave) {
    dimensionSaves.put(dimensionName, dimensionSave);
  }

  public static void deleteDimension(Identifier dimensionIdentifier) {
    RegistryKey<World> dimensionKey = RegistryKey.of(RegistryKeys.WORLD, dimensionIdentifier);
    ServerWorld dimension = minecraftServer.getWorld(dimensionKey);
    MinecraftServerAccessor serverAccess = (MinecraftServerAccessor)(minecraftServer);

    // Call unload to allow other mods/places to listen to unload events
    ServerWorldEvents.UNLOAD.invoker().onWorldUnload(minecraftServer, dimension);

    // Then we can remove the world from the world list
    if (serverAccess.getWorlds().remove(dimensionKey, dimension)) {
      dimensionSaves.get(dimensionIdentifier).deleteConfigFiles();
      dimensionSaves.remove(dimensionKey.getValue());

      // remove from generated list to prevent attenpts to create it on restart
      DimensionSave mainSave = DimensionManager.getOrCreateDimensionSave(minecraftServer.getWorld(World.OVERWORLD));
      mainSave.removeGeneratedWorld(dimensionIdentifier);

      LevelStorage.Session session = serverAccess.getSession();
      File worldDirectory = session.getWorldDirectory(dimensionKey).toFile();
      Plunger.info("Attempting to delete Dir: " + worldDirectory);
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
    } else {
      Plunger.error("Unable to remove dimension from worlds list: " + dimensionIdentifier);
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
    // dimensionWorld.getWorldBorder().setMaxRadius(4);
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

    MinecraftServerAccessor serverAccess = (MinecraftServerAccessor)(server);
    Session session = serverAccess.getSession();
    String minecraftFolder = session.getDirectory(WorldSavePath.ROOT).toString();
    Path standAloneDimensions = Paths.get(minecraftFolder, "sandbox", "dimensions", "data");
    File dimensionsDataFolder = standAloneDimensions.toFile();
    if (dimensionsDataFolder.exists()) {
      File[] namespaces = dimensionsDataFolder.listFiles((dir, name) -> dir.isDirectory());
      for (File namespaceFolder : namespaces) {
        Plunger.info("Namespace: " + namespaceFolder.getName());
        // Check for sandbox_dimension files
        File sandboxDimensionFolder = Paths.get(
          namespaceFolder.toPath().toString(),
          DimensionSave.DIMENSION_CONFIG_FOLDER
        ).toFile();

        if (sandboxDimensionFolder.exists()) {
          File[] configFiles = sandboxDimensionFolder.listFiles((file) -> file.getName().endsWith(".json"));
          for (File configFile : configFiles) {
            Identifier dimensionIdentifier = new Identifier(
              namespaceFolder.getName(),
              configFile.getName().replaceAll(".json", "")
            );

            InputStream inputStream;
            try {
              inputStream = new FileInputStream(configFile);
              byte[] buffer = new byte[inputStream.available()];
              inputStream.read(buffer);
              inputStream.close();
              String jsonString = new String(buffer, "UTF-8");
              DatapackDimensionConfig dimensionConfig = gson.fromJson(jsonString, DatapackDimensionConfig.class);
              Plunger.info("Options: " + dimensionConfig.getDimensionOptionsId());
              SandboxWorldConfig sandboxConfig = new SandboxWorldConfig(server, dimensionConfig);
              DimensionSave dimensionSave = DimensionManager.buildDimensionSaveFromConfig(dimensionIdentifier, sandboxConfig);
              dimensionSave.setDimensionConfigPath(configFile.toPath());
            } catch (FileNotFoundException e) {
              Plunger.error("Config File not found", e);
            } catch (IOException e) {
              Plunger.error("Issue reading in Config file", e);
            }

          }
        }
      }
    }
  }

  public static void unload() {
    dimensionSaves = new HashMap<>();
    minecraftServer = null;
  } 
}
