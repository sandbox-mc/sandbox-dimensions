package io.sandboxmc.dimension.configs;

import net.minecraft.util.Identifier;
import net.minecraft.world.Difficulty;

public class DatapackDimensionConfig {
  // Should spin up on server start?
  // if false, it gets generated from a different trigger
  // such as a command or commandBlock or a mod
  public Boolean initializeOnStartup;

  // Should save to datapack
  // allows for sharing to other servers or personal worlds
  public Boolean shouldSaveWorld = true;

  // Should allow instancing?
  // if shouldSave is true, this is ignored
  // What about if we just have a createInstance() method that clones it and doens't save?
  // do we really need this flag?
  public Boolean canInstance = false;

  // Should remove on player leave
  // ignored if shouldSave is true
  // if true, we can't delete something that should be written back to the datapack
  public Boolean removeOnPlayerLeave = false;

  // Should generate seed
  // if false, this just uses the overworld seed or passed seed if populated
  public Boolean generateSeed = false;

  private String difficulty = null;
  public Difficulty getDifficulty() {
    return Difficulty.byName(difficulty);
  }
  public void setDifficulty(Difficulty difficult) {
    difficulty = difficult.toString();
  }
  
  // DimensionType to use to spin up
  // Examples: ["minecraft:overworld", "sandboxmc:empty"]
  // This will fail if DimensionOptions do not exists
  private String dimensionOptionsId;
  public Identifier getDimensionOptionsId() {
    return dimensionOptionsId != null ? new Identifier(dimensionOptionsId): null;
  }
  public void setDimensionOptionsId(Identifier identifier) {
    dimensionOptionsId = identifier.toString();
  }

  // The dimension rules, defined in /data/<namespace>/dimension_type/<type>.json file of a datapack;
  // Example: <namespace>:<dimension_type>, minecraft:overworld
  private String dimensionType;
  public Identifier getDimensionType() {
    return dimensionType != null ? new Identifier(dimensionType) : null;
  }
  public void setDimensionType(Identifier identifier) {
    dimensionType = identifier.toString();
  }

  // Is flat world or not
  // must be initialized
  public Boolean flat;
  // public final GameRuleOverrides gameRules = new GameRuleOverrides();
  public boolean raining;
  public int rainTime = 0;
  public long seed = 0;
  public boolean shouldTickTime = true;
  public int sunnyTime;
  public boolean thundering;
  public int thunderTime;
  public long timeOfDay;
}
