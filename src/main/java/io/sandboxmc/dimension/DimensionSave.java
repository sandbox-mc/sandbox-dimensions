package io.sandboxmc.dimension;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.apache.commons.io.FileUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;

import io.sandboxmc.Plunger;
import io.sandboxmc.datapacks.Datapack;
import io.sandboxmc.datapacks.DatapackManager;
import io.sandboxmc.dimension.configs.DatapackDimensionConfig;
import io.sandboxmc.player.PlayerData;
import io.sandboxmc.zip.ZipUtility;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.resource.Resource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;

public class DimensionSave extends PersistentState {
  // Global Constants
  public static final String DIMENSION_CONFIG_FOLDER = "sandbox_dimension";
  public static final String WORLD_SAVE_FOLDER = "saves";

  // Game Rules
  public static final String KEEP_INVENTORY_ON_DEATH = "keepInventoryOnDeath";
  public static final String KEEP_INVENTORY_ON_JOIN = "keepInventoryOnJoin";
  public static final String RESPAWN_IN_DIMENSION = "respawnInDimension";
  public static List<String> GAME_RULES = List.of(
    KEEP_INVENTORY_ON_DEATH,
    KEEP_INVENTORY_ON_JOIN,
    RESPAWN_IN_DIMENSION
  );
  
  // World Data save names
  private static String DIMENSION_IS_ACTIVE = "dimensionIsActive";
  private static String DIMENSION_SAVE_LOADED = "dimensionSaveLoaded";
  private static String GENERATED_WORLDS = "generatedWorlds";
  private static String PLAYERS = "players";
  private static String SPAWN_ANGLE = "SpawnAngle";
  private static String SPAWN_X = "SpawnX";
  private static String SPAWN_Y = "SpawnY";
  private static String SPAWN_Z = "SpawnZ";

  private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

  // Class props
  private SandboxWorldConfig sandboxWorldConfig;
  private String datapackName;
  public Boolean dimensionIsActive = false;
  public Boolean dimensionSaveLoaded = false;
  private Identifier identifier;
  public Boolean keepInventoryOnDeath = false;
  public Boolean keepInventoryOnJoin = true;
  public Boolean respawnInDimension = false;
  private float spawnAngle = 0f;
  private int spawnX = 0;
  private int spawnY = 0;
  private int spawnZ = 0;
  private ServerWorld serverWorld;

  // File paths
  private Path dimensionConfigPath;
  private Path dimensionPath;
  private Path savefilePath;

  // generatedWorlds is specific to the Overworld and is used to save stuff
  public HashMap<Identifier, Identifier> generatedWorlds = new HashMap<>();
  public HashMap<UUID, PlayerData> players = new HashMap<>();
 
  @Override
  public NbtCompound writeNbt(NbtCompound nbt) {
    nbt.putBoolean(DIMENSION_IS_ACTIVE, dimensionIsActive);
    nbt.putBoolean(DIMENSION_SAVE_LOADED, dimensionSaveLoaded);
    nbt.putBoolean(KEEP_INVENTORY_ON_DEATH, keepInventoryOnDeath);
    nbt.putBoolean(KEEP_INVENTORY_ON_JOIN, keepInventoryOnJoin);
    nbt.putBoolean(RESPAWN_IN_DIMENSION, respawnInDimension);
    nbt.putFloat(SPAWN_ANGLE, spawnAngle);
    nbt.putInt(SPAWN_X, spawnX);
    nbt.putInt(SPAWN_Y, spawnY);
    nbt.putInt(SPAWN_Z, spawnZ);

    // Populate GeneratedWorlds list
    NbtCompound generatedWorldsCollection = new NbtCompound();
    generatedWorlds.forEach((Identifier dimensionId, Identifier dimensionOptionsId) -> {
      generatedWorldsCollection.putString(dimensionId.toString(), dimensionOptionsId.toString());
    });
    nbt.put(GENERATED_WORLDS, generatedWorldsCollection);

    // Populate Player data
    // this is only populated for Overworld
    NbtCompound playerDataCollection = new NbtCompound();
    players.forEach((uuid, playerData) -> {
      playerDataCollection.put(uuid.toString(), playerData.writePlayerDataNbt());
    });
    nbt.put(PLAYERS, playerDataCollection);
    return nbt;
  }

  public static DimensionSave createFromNbt(NbtCompound tag) {
    DimensionSave state = new DimensionSave();
    state.dimensionIsActive = tag.getBoolean(DIMENSION_IS_ACTIVE);
    state.dimensionSaveLoaded = tag.getBoolean(DIMENSION_SAVE_LOADED);
    state.keepInventoryOnDeath = tag.getBoolean(KEEP_INVENTORY_ON_DEATH);
    state.keepInventoryOnJoin = tag.getBoolean(KEEP_INVENTORY_ON_JOIN);
    state.respawnInDimension = tag.getBoolean(RESPAWN_IN_DIMENSION);
    state.spawnAngle = tag.getFloat(SPAWN_ANGLE);
    state.spawnX = tag.getInt(SPAWN_X);
    state.spawnY = tag.getInt(SPAWN_Y);
    state.spawnZ = tag.getInt(SPAWN_Z);

    // Read GeneratedWorld data from nbt data
    NbtCompound generatedWorldsNbt = tag.getCompound(GENERATED_WORLDS);
    generatedWorldsNbt.getKeys().forEach(key -> {
      state.generatedWorlds.put(
        new Identifier(key),
        new Identifier(generatedWorldsNbt.getString(key))
      );
    });

    // Read player data from nbt data
    NbtCompound playersNbt = tag.getCompound(PLAYERS);
    playersNbt.getKeys().forEach(key -> {
      PlayerData playerData = new PlayerData();
      playerData.readFromNbt(playersNbt.getCompound(key));
      state.players.put(UUID.fromString(key), playerData);
    });

    return state;
  }

  public void deleteConfigFiles() {
    this.dimensionConfigPath.toFile().delete();
    this.dimensionPath.toFile().delete();
  }

  public Boolean generateConfigFiles() {
    Path basePath = DatapackManager.getDimensionStorageFolder();
    // if has datapack
    if (this.datapackName != null) {
      Datapack datapack = DatapackManager.getDatapack(this.datapackName);
      if (datapack != null) {
        basePath = datapack.getDatapackPath();
      } else {
        Plunger.error("Missing expected datapack: " + this.datapackName);
        return false;
      }
    }

    Path namespacePath = Paths.get(basePath.toString(), "data", identifier.getNamespace());

    // Write or move the files
    writeOrMoveConfigFile(namespacePath);
    writeOrMoveDimensionFile(namespacePath);

    return true;
  }

  private Boolean writeOrMoveConfigFile(Path namespacePath) {
    Path newDimensionConfigPath = this.ensureFolderAndGetPath(
      Paths.get(namespacePath.toString(), DIMENSION_CONFIG_FOLDER),
      identifier.getPath() + ".json"
    );

    if (this.dimensionConfigPath == null) {
      SandboxWorldConfig worldConfig = ((SandboxWorld)this.getServerWorld()).getConfig();
      DatapackDimensionConfig dimensionConfig = worldConfig.buildDatapackConfig();

      try {
        String jsonString = gson.toJson(dimensionConfig);
        gson.toJson(dimensionConfig, new FileWriter(newDimensionConfigPath.toString()));
        FileWriter file = new FileWriter(newDimensionConfigPath.toString());
        file.write(jsonString);
        file.close();
        this.dimensionConfigPath = newDimensionConfigPath;
      } catch (JsonIOException | IOException e) {
        Plunger.error("Failed to Create config for: " + identifier, e);
      }
    } else if (!this.dimensionConfigPath.equals(newDimensionConfigPath)) {
      // we need to move the files and not write them
      this.dimensionConfigPath.toFile().renameTo(new File(newDimensionConfigPath.toString()));
      this.dimensionConfigPath = newDimensionConfigPath;
    }

    return true;
  }

  private Boolean writeOrMoveDimensionFile(Path namespacePath) {
    SandboxWorldConfig worldConfig = ((SandboxWorld)this.getServerWorld()).getConfig();
    Identifier dimensionOptionsId = worldConfig.getDimensionOptions().dimensionTypeEntry().getKey().get().getValue();
    Optional<Resource> dimensionResourceOptional = this.getServerWorld().getServer().getResourceManager().getResource(dimensionOptionsId);
    if (dimensionResourceOptional.isPresent()) {
      // no need to create the director if they are using a default dimensionOption such as minecraft:overworld
      Path dimensionPath = this.ensureFolderAndGetPath(
        Paths.get(namespacePath.toString(), "dimension"),
        identifier.getPath() + ".json"
      );

      // Else the dimensionOption is from a default type and should always be there
      try (InputStream inputStream = dimensionResourceOptional.get().getInputStream()) {
        File newFile = new File(dimensionPath.toString());
        if (newFile.exists()) {
          // Not sure if we should delete the file here...
          // Technically... there shouldn't be one there
          // And we are always cloning the original
          // newFile.delete();
          // newFile = new File(dimensionPath.toString());
          Plunger.error("File already exists " + identifier);
          return false;
        }

        FileUtils.copyInputStreamToFile(inputStream, newFile);
      } catch (IOException e) {
        Plunger.error("Failed to Write Json for: " + identifier, e);
      }
    }

    return true;
  }

  public Path ensureFolderAndGetPath(Path folderPath, String fileName) {
    File newFolder = folderPath.toFile();
    if (!newFolder.exists()) {
      newFolder.mkdirs();
    }

    return Paths.get(folderPath.toString(), fileName);
  }

  public void addGeneratedWorld(Identifier dimensionId, Identifier dimensionOptionsId) {
    this.generatedWorlds.put(dimensionId, dimensionOptionsId);
  }

  public String getDatapackName() {
    return this.datapackName;
  }

  public Identifier getDimensionIdentifier() {
    return this.identifier;
  }

  public Path getDimensionConfigPath() {
    return this.dimensionConfigPath;
  }

  public Path getDimensionPath() {
    return this.dimensionPath;
  }

  public HashMap<Identifier, Identifier> getGeneratedWorlds() {
    return this.generatedWorlds;
  }

  public PlayerData getPlayerData(PlayerEntity player) {
    return this.players.computeIfAbsent(player.getUuid(), uuid -> new PlayerData());
  }

  public Boolean getRule(String ruleName) {
    switch (ruleName) {
      case KEEP_INVENTORY_ON_DEATH:
        return this.keepInventoryOnDeath;
      case KEEP_INVENTORY_ON_JOIN:
        return this.keepInventoryOnJoin;
      case RESPAWN_IN_DIMENSION:
        return this.respawnInDimension;
    }

    return null;
  }

  public ServerWorld getServerWorld() {
    return this.serverWorld;
  }

  public BlockPos getSpawnPos(ServerWorld dimension) {
    BlockPos blockPos = new BlockPos(this.getSpawnX(), this.getSpawnY(), this.getSpawnZ());
    if (!dimension.getWorldBorder().contains(blockPos)) {
      blockPos = dimension.getTopPosition(
        net.minecraft.world.Heightmap.Type.MOTION_BLOCKING,
        BlockPos.ofFloored(dimension.getWorldBorder().getCenterX(), 0.0D, dimension.getWorldBorder().getCenterZ())
      );
    }

    return blockPos;
  }

  public int getSpawnX() {
    return this.spawnX;
  }

  public int getSpawnY() {
    return this.spawnY;
  }

  public int getSpawnZ() {
    return this.spawnZ;
  }

  // Attempts to load a saveFile in the datapack if exists
  public Boolean loadDimensionFile() {
    Datapack datapack = DatapackManager.getDatapack(this.datapackName);
    if (datapack == null) {
      // If there is no datapack there is no save file
      return false;
    }

    String dimensionNamespace = this.identifier.getNamespace();
    String dimensionName = this.identifier.getPath();

    // Path to load dimension save
    Path savePath = Paths.get(
      "data",
      dimensionNamespace,
      WORLD_SAVE_FOLDER,
      dimensionName + ".zip" // File name should be dimensionName + zip
    );
    Path datapackLoadFilePath = Paths.get(
      datapack.getDatapackPath().toString(),
      savePath.toString()
    );

    // if there is no save file, this process fails
    if (!datapackLoadFilePath.toFile().exists()) {
      return false;
    }

    // Path the the save dir...
    Path dimensionSavePath = Paths.get(
      datapack.getDatapackPath().getParent().getParent().toString(),
      "dimensions",
      dimensionNamespace,
      dimensionName
    );

    // Create the path if it doesn't exist
    File dimensionFile = dimensionSavePath.toFile();
    if (!dimensionFile.exists()) {
      dimensionFile.mkdirs();
    }

    try {
      
      // delete current dimension save
      // Maybe make this a backup process or something???
      ZipUtility.deleteDirectory(dimensionSavePath);

      // Unzip the world files
      Plunger.debug("Starting unzip for: " + this.identifier);
      ZipUtility.unzipFile(datapackLoadFilePath, dimensionSavePath);
      Plunger.debug("Done unzipping for: " + this.identifier);

      // if we load a save file, it's an active dimension
      this.dimensionSaveLoaded = true;
      DimensionManager.addDimensionSave(this.identifier, this);

      return true;
    } catch (IOException e) {
      Plunger.error("Failed in loadDimensionFile!", e);
    }

    return false;
  }

  public DimensionSave removeDatapack() {
    this.datapackName = null;
    return this;
  }

  public void removeGeneratedWorld(Identifier dimensionId) {
    this.generatedWorlds.remove(dimensionId);
  }

  public void setDatapackName(String name) {
    this.datapackName = name;
  }

  public void setDimensionConfigPath(Path dimensionConfigPath) {
    this.dimensionConfigPath = dimensionConfigPath;
  }

  public void setDimensionPath(Path dimensionPath) {
    this.dimensionPath = dimensionPath;
  }

  public DimensionSave setIdentifer(Identifier identifier) {
    this.identifier = identifier;
    return this;
  }

  public void setPlayerData(UUID uuid, PlayerData playerData) {
    players.put(uuid, playerData);
  }
  
  public void setRule(String ruleName, Boolean value) {
    switch (ruleName) {
      case KEEP_INVENTORY_ON_DEATH:
      this.keepInventoryOnDeath = value;
      break;
      case KEEP_INVENTORY_ON_JOIN:
      this.keepInventoryOnJoin = value;
      break;
      case RESPAWN_IN_DIMENSION:
      this.respawnInDimension = value;
      break;
    }
  }

  public void setSaveFile(Path saveFilePath) {
    this.savefilePath = saveFilePath;
  }

  public DimensionSave setServerWorld(ServerWorld serverWorld) {
    this.serverWorld = serverWorld;
    return this;
  }

  public Boolean setSpawnPos(ServerWorld dimension, BlockPos blockPos) {
    if (!dimension.getWorldBorder().contains(blockPos)) {
      return false;
    }

    // TODO:BRENT think about spawnAngle
    this.spawnX = blockPos.getX();
    this.spawnY = blockPos.getY();
    this.spawnZ = blockPos.getZ();
    this.markDirty();

    return true;
  }

  // Swaps inventory if the player has a different inventory in next dimension
  // Empties invenotry if not
  // Saves Original Inventory
  public void swapPlayerInventoryWithDestination(ServerPlayerEntity player) {
    DimensionSave originalDimensionSave = DimensionManager.getOrCreateDimensionSave(player.getServerWorld());
    PlayerData originalDimensionPlayerData = originalDimensionSave.getPlayerData(player);
    PlayerData destinationPlayerData = this.getPlayerData(player);
    PlayerInventory playerInventory = player.getInventory();

    // Create a cache inventory if one doesn't exist
    if (originalDimensionPlayerData.inventoryCache == null) {
      originalDimensionPlayerData.inventoryCache = new PlayerInventory(null);
    }
    
    // Clone and cache the inventory
    originalDimensionPlayerData.inventoryCache.clone(playerInventory);

    // Clear the Player inventory so they enter empty
    playerInventory.clear();

    // If the destination has an inventory... load it?
    if (destinationPlayerData.inventoryCache != null) {
      Plunger.debug("HAD INV");
      playerInventory.clone(destinationPlayerData.inventoryCache);
    }
    originalDimensionSave.markDirty();
    this.markDirty();
  }
}
