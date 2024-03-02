package io.sandboxmc.dimension;

import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameRules;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.DimensionTypes;
import net.minecraft.world.gen.chunk.ChunkGenerator;

public class SandboxWorldConfig {
  private ChunkGenerator generator = null;
  private Difficulty difficulty = null;
  private DimensionOptions dimensionOptions;
  private RegistryEntry<DimensionType> dimensionType;
  private RegistryKey<DimensionType> dimensionTypeKey = DimensionTypes.OVERWORLD;
  private TriState flat = TriState.DEFAULT;
  private final GameRuleOverrides gameRules = new GameRuleOverrides();
  private boolean raining;
  private int rainTime;
  private long seed = 0;
  private boolean shouldTickTime = true;
  private int sunnyTime = Integer.MAX_VALUE;
  private boolean thundering;
  private int thunderTime;
  private long timeOfDay = 6000;
  private SandboxWorld.Constructor worldConstructor = SandboxWorld::new;
  // List<SpecialSpawner> list = new ArrayList<SpecialSpawner>();


  public DimensionOptions createDimensionOptions(MinecraftServer server) {
    // default to dimensionOtions if set
    if (dimensionOptions != null) {
      return this.dimensionOptions;
    }

    var dimensionType = this.resolveDimensionType(server);
    var dimOptions = server.getRegistryManager().get(RegistryKeys.DIMENSION).get(new Identifier("overworld"));
    if (this.generator == null) {
      if (dimOptions != null) {
        // get default ChunkGenerator for Overworld
        this.generator = dimOptions.chunkGenerator();
      } else {
        // something is super broken...
      }
    }

    return new DimensionOptions(dimensionType, this.generator);
  }

  public Difficulty getDifficulty() {
    return this.difficulty;
  }

  public TriState getFlat() {
    return this.flat;
  }

  public GameRuleOverrides getGameRules() {
    return this.gameRules;
  }

  public int getSunnyTime() {
    return this.sunnyTime;
  }

  public int getRainTime() {
    return this.rainTime;
  }

  public int getThunderTime() {
    return this.thunderTime;
  }

  public long getTimeOfDay() {
    return this.timeOfDay;
  }

  public boolean getRaining() {
    return this.raining;
  }

  public long getSeed() {
    return seed;
  }

  public boolean getThundering() {
    return this.thundering;
  }

  public boolean getShouldTickTime() {
    return this.shouldTickTime;
  }

  public SandboxWorld.Constructor getWorldConstructor() {
    return this.worldConstructor;
  }

  private RegistryEntry<DimensionType> resolveDimensionType(MinecraftServer server) {
    var dimensionType = this.dimensionType;
    if (dimensionType == null) {
      dimensionType = server.getRegistryManager().get(RegistryKeys.DIMENSION_TYPE).getEntry(this.dimensionTypeKey).orElse(null);
      if (dimensionType == null) {
        System.out.println("invalid dimension type " + this.dimensionTypeKey);
      }
    }

    return dimensionType;
  }

  public SandboxWorldConfig setDifficulty(Difficulty difficulty) {
    this.difficulty = difficulty;
    return this;
  }

  public SandboxWorldConfig setDimensionOptions(DimensionOptions dimensionOptions) {
    this.dimensionOptions = dimensionOptions;
    return this;
  }

  public SandboxWorldConfig setDimensionType(RegistryEntry<DimensionType> dimensionType) {
    this.dimensionType = dimensionType;
    this.dimensionTypeKey = null;
    return this;
  }

  public SandboxWorldConfig setDimensionType(RegistryKey<DimensionType> dimensionType) {
    this.dimensionType = null;
    this.dimensionTypeKey = dimensionType;
    return this;
  }

  public SandboxWorldConfig setFlat(TriState state) {
    this.flat = state;
    return this;
  }

  public SandboxWorldConfig setFlat(boolean state) {
    return this.setFlat(TriState.of(state));
  }

  public SandboxWorldConfig setGameRule(GameRules.Key<GameRules.BooleanRule> key, boolean value) {
    this.gameRules.set(key, value);
    return this;
  }

  public SandboxWorldConfig setGameRule(GameRules.Key<GameRules.IntRule> key, int value) {
    this.gameRules.set(key, value);
    return this;
  }

  public SandboxWorldConfig setGenerator(ChunkGenerator generator) {
    this.generator = generator;
    return this;
  }

  public SandboxWorldConfig setSeed(long updatedSeed) {
    this.seed = updatedSeed;
    return this;
  }

  public SandboxWorldConfig setRaining(int rainTime) {
    this.raining = rainTime > 0;
    this.rainTime = rainTime;
    return this;
  }

  public SandboxWorldConfig setRaining(boolean raining) {
    this.raining = raining;
    return this;
  }

  public SandboxWorldConfig setSunny(int sunnyTime) {
    this.sunnyTime = sunnyTime;
    this.raining = false;
    this.thundering = false;
    return this;
  }

  public SandboxWorldConfig setThundering(int thunderTime) {
    this.thundering = thunderTime > 0;
    this.thunderTime = thunderTime;
    return this;
  }

  public SandboxWorldConfig setThundering(boolean thundering) {
    this.thundering = thundering;
    return this;
  }

  public SandboxWorldConfig setTimeOfDay(long time) {
    this.timeOfDay = time;
    return this;
  }
}
