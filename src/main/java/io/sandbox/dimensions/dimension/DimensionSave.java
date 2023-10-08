package io.sandbox.dimensions.dimension;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.sandbox.dimensions.Main;
import io.sandbox.dimensions.dimension.zip.UnzipUtility;
import io.sandbox.dimensions.mixin.MinecraftServerAccessor;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.PersistentState;
import net.minecraft.world.PersistentStateManager;
import net.minecraft.world.level.storage.LevelStorage.Session;

public class DimensionSave extends PersistentState {
  private static String DIMENSION_IS_ACTIVE = "dimensionIsActive";
  private static String DIMENSION_SAVE_LOADED = "dimensionSaveLoaded";
  public Boolean dimensionIsActive = false;
  public Boolean dimensionSaveLoaded = false;
 
  @Override
  public NbtCompound writeNbt(NbtCompound nbt) {
      nbt.putBoolean(DIMENSION_IS_ACTIVE, dimensionIsActive);
      nbt.putBoolean(DIMENSION_SAVE_LOADED, dimensionSaveLoaded);
      return nbt;
  }

  public static DimensionSave createFromNbt(NbtCompound tag) {
    DimensionSave state = new DimensionSave();
    state.dimensionIsActive = tag.getBoolean(DIMENSION_IS_ACTIVE);
    state.dimensionSaveLoaded = tag.getBoolean(DIMENSION_SAVE_LOADED);
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

  public void loadSaveFile(String datapackNameString, MinecraftServer server, Boolean override) {
    if (this.dimensionSaveLoaded && !override) {
      // If we have already loaded this saved world, ignore, unless it was overriden
      return;
    };

    // The Session gets directory context into the specific save dir in run
    // /run/<my-save-name>
    Session session = ((MinecraftServerAccessor)server).getSession();
    Identifier dataPackId = new Identifier(datapackNameString);
    String dataPackName = dataPackId.getNamespace();
    String dimensionName = dataPackId.getPath();

    // Path the the save dir...
    Path dimensionSavePath = Paths.get(
      session.getDirectory(WorldSavePath.DATAPACKS).getParent().toString(),
      "dimensions",
      dataPackName,
      dimensionName
    );
    Path datapackLoadFilePath = Paths.get(
      session.getDirectory(WorldSavePath.DATAPACKS).toString(),
      dataPackName,
      "data",
      dataPackName,
      "world_saves",
      dimensionName + ".zip" // File name should be dimensionName + zip
    );
        
    System.out.println("DataPack Dir: " + datapackLoadFilePath); // full path

    try {
      
      // delete current dimension save
      // Maybe make this a backup process or something???
      UnzipUtility.deleteDirectory(dimensionSavePath);

      // Unzip the world files
      System.out.println("Starting unzip");
      UnzipUtility.unzipFile(datapackLoadFilePath, dimensionSavePath);
      System.out.println("Done unzipping");
      this.dimensionSaveLoaded = true;
      this.markDirty(); // flags the state change for save on server restart
    } catch (IOException e) {
      System.out.println("IO Exception");
      e.printStackTrace();
    }
  }
}
