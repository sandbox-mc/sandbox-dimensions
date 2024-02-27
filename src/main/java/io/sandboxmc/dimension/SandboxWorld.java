package io.sandboxmc.dimension;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ImmutableList;

import io.sandboxmc.mixin.MinecraftServerAccessor;
import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ProgressListener;
import net.minecraft.util.Util;
import net.minecraft.world.World;

public class SandboxWorld extends ServerWorld {
  final Boolean isPermanentWorld;
  private boolean flat;

  public SandboxWorld(MinecraftServer server, RegistryKey<World> registryKey, SandboxWorldConfig config, Boolean isPermanentWorld) {
    super(
      server,
      Util.getMainWorkerExecutor(),
      ((MinecraftServerAccessor) server).getSession(),
      new SandboxWorldProperties(server.getSaveProperties(), config),
      registryKey,
      config.createDimensionOptions(server),
      ((MinecraftServerAccessor) server).getWorldGenerationProgressListenerFactory().create(11),
      false,
      config.getSeed(),
      ImmutableList.of(),
      config.getShouldTickTime(),
      null
    );

    this.isPermanentWorld = isPermanentWorld;
    this.flat = config.getFlat().orElse(super.isFlat());
  }

  @Override
  public long getSeed() {
    return ((SandboxWorldProperties) this.properties).config.getSeed();
  }

  @Override
  public boolean isFlat() {
    return this.flat;
  }

  @Override
  public void save(@Nullable ProgressListener progressListener, boolean flush, boolean enabled) {
  if (!this.isPermanentWorld || !flush) {
    super.save(progressListener, flush, enabled);
  }
  }
  
  public interface Constructor {
    SandboxWorld createWorld(MinecraftServer server, RegistryKey<World> registryKey, SandboxWorldConfig config, Boolean isPermanentWorld);
  }
}
