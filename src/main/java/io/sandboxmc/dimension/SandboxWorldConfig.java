package io.sandboxmc.dimension;

import io.sandboxmc.Plunger;
import io.sandboxmc.datapacks.DatapackManager;
import io.sandboxmc.dimension.configs.DatapackDimensionConfig;
import net.fabricmc.fabric.api.util.TriState;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.Identifier;
import net.minecraft.world.Difficulty;
import net.minecraft.world.GameRules;
import net.minecraft.world.World;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.dimension.DimensionTypes;
import net.minecraft.world.gen.GeneratorOptions;
import net.minecraft.world.gen.chunk.ChunkGenerator;

public class SandboxWorldConfig {
  private ChunkGenerator generator = null;
  private Difficulty difficulty = null;
  private DimensionOptions dimensionOptions;
  private Identifier dimensionOptionsId = new Identifier("overworld");
  private RegistryEntry<DimensionType> dimensionType;
  private RegistryKey<DimensionType> dimensionTypeKey = DimensionTypes.OVERWORLD;
  private TriState flat = TriState.DEFAULT;
  private final GameRuleOverrides gameRules = new GameRuleOverrides();
  private boolean generateSeed = false;
  private Boolean initializeOnStartup;
  private boolean raining;
  private int rainTime;
  private long seed = 0;
  private boolean seedHasBeenSet = false;

  private MinecraftServer server;
  private boolean shouldSaveWorld = true;
  private boolean shouldTickTime = true;
  private int sunnyTime = Integer.MAX_VALUE;
  private boolean thundering;
  private int thunderTime;
  private long timeOfDay = 6000;
  private SandboxWorld.Constructor worldConstructor = SandboxWorld::new;
  // List<SpecialSpawner> list = new ArrayList<SpecialSpawner>();

  public SandboxWorldConfig(MinecraftServer server) {
    this.server = server;
  }

  public SandboxWorldConfig(MinecraftServer server, DatapackDimensionConfig datapackConfig) {
    this.server = server;
    this.difficulty = datapackConfig.getDifficulty() != null ?
      datapackConfig.getDifficulty() :
      server.getWorld(World.OVERWORLD).getDifficulty();
    this.flat = TriState.of(datapackConfig.flat);
    this.generateSeed = datapackConfig.generateSeed;
    this.shouldSaveWorld = datapackConfig.shouldSaveWorld;
    this.shouldTickTime = datapackConfig.shouldTickTime;

    Identifier dimensionOptionsIdentifier = datapackConfig.getDimensionOptionsId();
    if (dimensionOptionsIdentifier != null) {
      this.setDimensionOptionsId(dimensionOptionsIdentifier);
    }

    Identifier dimensionType = datapackConfig.getDimensionType();
    if (dimensionType != null) {
      this.setDimensionType(dimensionType);
    }

    if (datapackConfig.rainTime > 0) {
      this.setRaining(datapackConfig.rainTime);
    }

    if (datapackConfig.raining) {
      this.setRaining(datapackConfig.raining);
    }

    this.setDimensionOptions(this.createDimensionOptions());
  }

  public DatapackDimensionConfig buildDatapackConfig() {
    DatapackDimensionConfig datapackConfig = new DatapackDimensionConfig();
    datapackConfig.setDifficulty(this.difficulty != null ? this.difficulty : server.getWorld(World.OVERWORLD).getDifficulty());
    datapackConfig.setDimensionOptionsId(this.dimensionOptionsId);
    datapackConfig.setDimensionType(this.resolveDimensionType(DatapackManager.getServer()).getKey().get().getRegistry());
    datapackConfig.flat = this.flat.getBoxed();
    datapackConfig.generateSeed = this.generateSeed;
    datapackConfig.initializeOnStartup = this.initializeOnStartup;
    datapackConfig.rainTime = this.getRainTime();
    datapackConfig.raining = this.getRaining();
    // datapackConfig.removeOnPlayerLeave = 
    datapackConfig.seed = this.seed;
    datapackConfig.shouldSaveWorld = this.getShouldSaveWorld();
    datapackConfig.shouldTickTime = this.getShouldTickTime();
    datapackConfig.sunnyTime = this.getSunnyTime();
    datapackConfig.thunderTime = this.getThunderTime();
    datapackConfig.thundering = this.getThundering();
    datapackConfig.timeOfDay = this.getTimeOfDay();
    return datapackConfig;
  }

  public DimensionOptions createDimensionOptions() {
    // default to dimensionOtions if set
    if (dimensionOptions != null) {
      return this.dimensionOptions;
    }

    var dimensionType = this.resolveDimensionType(server);
    if (dimensionType == null) {
      Plunger.error("Missing DimensionType");
    }

    RegistryKey<DimensionOptions> dimensionRegistryKey = DimensionOptions.OVERWORLD;
    if (this.dimensionOptionsId != null) {
      dimensionRegistryKey = RegistryKey.of(RegistryKeys.DIMENSION, this.dimensionOptionsId);
    }

    this.dimensionOptions = this.server.getRegistryManager().get(RegistryKeys.DIMENSION).get(dimensionRegistryKey);
    if (this.generator != null) {
      // This is a programatical override for the generator
      this.dimensionOptions = new DimensionOptions(dimensionType, this.generator);
    }


    return this.dimensionOptions;
  }

  public Difficulty getDifficulty() {
    return this.difficulty;
  }

  public DimensionOptions getDimensionOptions() {
    if (this.dimensionOptions == null) {
      this.createDimensionOptions();
    }

    return this.dimensionOptions;
  }

  public Identifier getDimensionOptionsId() {
    return this.dimensionOptionsId;
  }

  public TriState getFlat() {
    return this.flat;
  }

  public GameRuleOverrides getGameRules() {
    return this.gameRules;
  }

  public Boolean getInitializeOnStart() {
    return this.initializeOnStartup;
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
    if (!this.seedHasBeenSet && this.generateSeed) {
      this.seedHasBeenSet = true;
      this.seed = GeneratorOptions.getRandomSeed();
    }

    return this.seed;
  }

  public boolean getThundering() {
    return this.thundering;
  }

  public boolean getShouldSaveWorld() {
    return this.shouldSaveWorld;
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
        Plunger.error("Invalid dimension type " + this.dimensionTypeKey + " using Overworld");
        return server.getRegistryManager().get(RegistryKeys.DIMENSION_TYPE).getEntry(DimensionTypes.OVERWORLD).orElse(null);
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

  public SandboxWorldConfig setDimensionOptionsId(Identifier dimensionOptionsIdentifier) {
    this.dimensionOptionsId = dimensionOptionsIdentifier;
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

  public SandboxWorldConfig setDimensionType(Identifier identifier) {
    this.dimensionType = null;
    this.dimensionTypeKey = RegistryKey.of(RegistryKeys.DIMENSION_TYPE, identifier);
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

  public SandboxWorldConfig setInitializeOnStartup(Boolean initializeOnStartup) {
    this.initializeOnStartup = initializeOnStartup;
    return this;
  }

  public SandboxWorldConfig setSeed(long updatedSeed) {
    this.seed = updatedSeed;
    this.seedHasBeenSet = true;
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

  public SandboxWorldConfig setShouldSaveWorld(boolean shouldSaveWorld) {
    this.shouldSaveWorld = shouldSaveWorld;
    return this;
  }

  public SandboxWorldConfig setShouldTickTime(boolean shouldTickTime) {
    this.shouldTickTime = shouldTickTime;
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
