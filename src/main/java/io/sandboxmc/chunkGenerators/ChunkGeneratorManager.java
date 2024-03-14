package io.sandboxmc.chunkGenerators;

import io.sandboxmc.SandboxMC;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class ChunkGeneratorManager {
  public static void init() {
    Registry.register(Registries.CHUNK_GENERATOR, SandboxMC.id("emptychunkgenerator"), EmptyChunkGenerator.CODEC);
  }
}
