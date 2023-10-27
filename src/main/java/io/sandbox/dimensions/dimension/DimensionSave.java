package io.sandbox.dimensions.dimension;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import io.sandbox.dimensions.Main;
import io.sandbox.dimensions.dimension.zip.ZipUtility;
import io.sandbox.dimensions.mixin.MinecraftServerAccessor;
import io.sandbox.dimensions.player.PlayerData;
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
  private static String DIMENSION_IS_ACTIVE = "dimensionIsActive";
  private static String DIMENSION_SAVE_LOADED = "dimensionSaveLoaded";
  public static final String KEEP_INVENTORY_ON_DEATH = "keepInventoryOnDeath";
  public static final String KEEP_INVENTORY_ON_JOIN = "keepInventoryOnJoin";
  public static final String RESPAWN_IN_DIMENSION = "respawnInDimension";
  public static List<String> GAME_RULES = List.of(
    KEEP_INVENTORY_ON_DEATH,
    KEEP_INVENTORY_ON_JOIN,
    RESPAWN_IN_DIMENSION
  );
  private static String PLAYERS = "players";
  private static String SPAWN_ANGLE = "SpawnAngle";
  private static String SPAWN_X = "SpawnX";
  private static String SPAWN_Y = "SpawnY";
  private static String SPAWN_Z = "SpawnZ";
  public Boolean dimensionIsActive = false;
  public Boolean dimensionSaveLoaded = false;
  public Boolean keepInventoryOnDeath = false;
  public Boolean keepInventoryOnJoin = true;
  public Boolean respawnInDimension = false;
  private float spawnAngle = 0f;
  private int spawnX = 0;
  private int spawnY = 0;
  private int spawnZ = 0;

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

    // Populate Player data
    // this is only populated for Overworld
    NbtCompound playerDataList = new NbtCompound();
    players.forEach((uuid, playerData) -> {
      playerDataList.put(uuid.toString(), playerData.writePlayerDataNbt());
    });
    nbt.put(PLAYERS, playerDataList);
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

  public static DimensionSave getDimensionState(ServerWorld dimension) {
    PersistentStateManager persistentStateManager = dimension.getPersistentStateManager();
    DimensionSave dimensionSave = persistentStateManager.getOrCreate(type, Main.modId);
    dimensionSave.markDirty();
    return dimensionSave;
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
  public void loadSaveFile(String dimensionIdentifierString, String packName, MinecraftServer server, Boolean override) {
    if (this.dimensionSaveLoaded && !override) {
      // If we have already loaded this saved world, ignore, unless it was overriden
      return;
    };

    // The Session gets directory context into the specific save dir in run
    // /run/<my-save-name>
    Session session = ((MinecraftServerAccessor)server).getSession();
    Identifier dataPackId = new Identifier(dimensionIdentifierString);
    String dataPackName = dataPackId.getNamespace();
    String dimensionName = dataPackId.getPath();

    // Path the the save dir...
    Path dimensionSavePath = Paths.get(
      session.getDirectory(WorldSavePath.DATAPACKS).getParent().toString(),
      "dimensions",
      dataPackName,
      dimensionName
    );
    
    // Path to load dimension save
    Path datapackLoadFilePath = Paths.get(
      session.getDirectory(WorldSavePath.DATAPACKS).toString(),
      packName,
      "data",
      dataPackName,
      "world_saves",
      dimensionName + ".zip" // File name should be dimensionName + zip
    );
        
    System.out.println("DataPack Dir: " + datapackLoadFilePath); // full path

    try {
      
      // delete current dimension save
      // Maybe make this a backup process or something???
      ZipUtility.deleteDirectory(dimensionSavePath);

      // Unzip the world files
      System.out.println("Starting unzip");
      ZipUtility.unzipFile(datapackLoadFilePath, dimensionSavePath);
      System.out.println("Done unzipping");
      this.dimensionSaveLoaded = true;
      // nbtCompound = NbtIo.readCompressed(path.toFile()); world.dat filepath
      this.markDirty(); // flags the state change for save on server restart
    } catch (IOException e) {
      System.out.println("IO Exception");
      e.printStackTrace();
    }
  }

  public static void saveDimensionToFile(ServerWorld dimension) {
    Session session = ((MinecraftServerAccessor)(dimension.getServer())).getSession();
    Path dimensionPath = session.getWorldDirectory(dimension.getRegistryKey());
    Identifier dimensionId = dimension.getRegistryKey().getValue();
    String dataPackName = DimensionManager.getPackFolder(dimensionId.toString());

    // Build sandboxPath
    Path sandboxPath = Paths.get(
      session.getDirectory(WorldSavePath.DATAPACKS).toString(),
      dataPackName,
      "data",
      dimensionId.getNamespace()
    );

    // make the dir if it doesn't exist
    if (!sandboxPath.toFile().exists()) {
      sandboxPath.toFile().mkdir();
    }

    // Next level create if it doesn't exist
    sandboxPath = Paths.get(sandboxPath.toString(), "saves", dimensionId.toString() + ".zip");
    // make the dir if it doesn't exist
    if (!sandboxPath.toFile().exists()) {
      sandboxPath.toFile().mkdir();
    }

    // ZipUtility.zipDirectory(dimensionPath.toFile(), sandboxPath.toString());

    System.out.println("Save: " + sandboxPath);
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

    // TODO: think about spawnAngle
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
    DimensionSave originalDimensionSave = DimensionSave.getDimensionState(player.getServerWorld());
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
