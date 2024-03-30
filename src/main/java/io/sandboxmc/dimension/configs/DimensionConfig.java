package io.sandboxmc.dimension.configs;

import net.minecraft.server.world.ServerWorld;

public class DimensionConfig {
  public String type;
  public GeneratorConfig generator;

  public static DimensionConfig create(ServerWorld server) {
    DimensionConfig config = new DimensionConfig();
    config.type = server.getDimensionKey().getValue().toString();
    config.generator = GeneratorConfig.create(server.getChunkManager().getChunkGenerator());
    System.out.println("Config: " + config.type);
    return new DimensionConfig();
  }
}