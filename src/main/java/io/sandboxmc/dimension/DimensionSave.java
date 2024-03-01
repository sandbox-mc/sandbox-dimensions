package io.sandboxmc.dimension;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import io.sandboxmc.Main;
import io.sandboxmc.mixin.MinecraftServerAccessor;
import io.sandboxmc.player.PlayerData;
import io.sandboxmc.zip.ZipUtility;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.level.storage.LevelStorage.Session;

public class DimensionSave extends PersistentState {
  // Global Constants
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

  // Class props
  public Boolean dimensionIsActive = false;
  public Boolean dimensionSaveLoaded = false;
  public Boolean keepInventoryOnDeath = false;
  public Boolean keepInventoryOnJoin = true;
  public Boolean respawnInDimension = false;
  private float spawnAngle = 0f;
  private int spawnX = 0;
  private int spawnY = 0;
  private int spawnZ = 0;

  // This IS used but the editor can't seem to figure out how.
  @SuppressWarnings("unused")
  private ServerWorld serverWorld;

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

  private static Type<DimensionSave> type = new Type<>(
    DimensionSave::new, // If there's no 'DimensionSave' yet create one
    DimensionSave::createFromNbt, // If there is a 'DimensionSave' NBT, parse it with 'createFromNbt'
    null // Supposed to be an 'DataFixTypes' enum, but we can just pass null
  );

  public static DimensionSave buildDimensionSave(ServerWorld dimension) {
    return buildDimensionSave(dimension, false);
  }

  // setActive is for initializing worlds as active
  public static DimensionSave buildDimensionSave(ServerWorld dimension, Boolean setActive) {
    Identifier dimensionId = dimension.getRegistryKey().getValue();
    DimensionSave dimensionSave = DimensionManager.getDimensionSave(dimensionId);
    if (dimensionSave != null) {
      return dimensionSave;
    }

    PersistentStateManager persistentStateManager = dimension.getPersistentStateManager();
    dimensionSave = persistentStateManager.getOrCreate(type, Main.modId);
    dimensionSave.serverWorld = dimension;
    if (dimensionSave.dimensionIsActive || setActive) {
      dimensionSave.dimensionIsActive = true;
      DimensionManager.addDimensionSave(dimensionId, dimensionSave);
    }

    dimensionSave.markDirty();
    return dimensionSave;
  }

  public void addGeneratedWorld(Identifier dimensionId, Identifier dimensionOptionsId) {
    this.generatedWorlds.put(dimensionId, dimensionOptionsId);
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

  // datapackNameString is dataPack:dimension format (can be the same name)
  public static Boolean loadDimensionFile(Identifier dimensionIdentifier, MinecraftServer server) {
    // The Session gets directory context into the specific save dir in run
    // /run/<my-save-name>
    Session session = ((MinecraftServerAccessor)server).getSession();
    String dimensionNamespace = dimensionIdentifier.getNamespace();
    String dimensionName = dimensionIdentifier.getPath();
    String packName = DimensionManager.getPackFolder(dimensionIdentifier.toString());
    Path datapacksPath = session.getDirectory(WorldSavePath.DATAPACKS);

    // Path to load dimension save
    Path datapackLoadFilePath = Paths.get(
      datapacksPath.toString(),
      packName,
      "data",
      dimensionNamespace,
      WORLD_SAVE_FOLDER,
      dimensionName + ".zip" // File name should be dimensionName + zip
    );

    // if there is no save file, this process fails
    if (!datapackLoadFilePath.toFile().exists()) {
      return false;
    }

    // Path the the save dir...
    Path dimensionSavePath = Paths.get(
      datapacksPath.getParent().toString(),
      "dimensions",
      dimensionNamespace,
      dimensionName
    );

    // Create the path if it doesn't exist
    File dimensionFile = dimensionSavePath.toFile();
    if (!dimensionFile.exists()) {
      dimensionFile.mkdirs();
    }

    System.out.println("DataPack Dir: " + datapackLoadFilePath); // full path

    try {
      
      // delete current dimension save
      // Maybe make this a backup process or something???
      ZipUtility.deleteDirectory(dimensionSavePath);

      // Unzip the world files
      System.out.println("Starting unzip");
      ZipUtility.unzipFile(datapackLoadFilePath, dimensionSavePath);
      System.out.println("Done unzipping");

      return true;
    } catch (IOException e) {
      System.out.println("IO Exception");
      e.printStackTrace();
    }

    return false;
  }

  public void removeGeneratedWorld(Identifier dimensionId) {
    this.generatedWorlds.remove(dimensionId);
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
    DimensionSave originalDimensionSave = DimensionSave.buildDimensionSave(player.getServerWorld());
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
      System.out.println("HAD INV");
      playerInventory.clone(destinationPlayerData.inventoryCache);
    }
    originalDimensionSave.markDirty();
    this.markDirty();
  }
}
