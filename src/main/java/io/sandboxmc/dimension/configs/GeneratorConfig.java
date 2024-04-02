package io.sandboxmc.dimension.configs;

import io.sandboxmc.Plunger;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.NoiseChunkGenerator;

public class GeneratorConfig {
  public String type;
  public String settings;
  public BiomeSourceConfig biome_source;

  public static GeneratorConfig create(ChunkGenerator chunkGenerator) {
    GeneratorConfig chunkGeneratorConfig = new GeneratorConfig();
    if (chunkGenerator instanceof NoiseChunkGenerator) {
      chunkGeneratorConfig.type = "minecraft:noise";
      chunkGeneratorConfig.settings = ((NoiseChunkGenerator)chunkGenerator).getSettings().getKey().get().getValue().toString();
    } else {
      chunkGeneratorConfig.type = "minecraft:flat";
    }
    Plunger.debug("ChunkConfig: " + chunkGeneratorConfig.settings);
    return chunkGeneratorConfig;
  }
}
