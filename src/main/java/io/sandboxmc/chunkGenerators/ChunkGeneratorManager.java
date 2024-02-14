package io.sandboxmc.chunkGenerators;

import io.sandboxmc.Main;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class ChunkGeneratorManager {
  public static void init() {
    Registry.register(Registries.CHUNK_GENERATOR, Main.id("emptychunkgenerator"), EmptyChunkGenerator.CODEC);
  }
}
