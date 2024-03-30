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
  private SandboxWorldConfig config;
  private boolean flat;

  public SandboxWorld(MinecraftServer server, RegistryKey<World> registryKey, SandboxWorldConfig config) {
    super(
      server,
      Util.getMainWorkerExecutor(),
      ((MinecraftServerAccessor) server).getSession(),
      new SandboxWorldProperties(server.getSaveProperties(), config),
      registryKey,
      config.getDimensionOptions(),
      ((MinecraftServerAccessor) server).getWorldGenerationProgressListenerFactory().create(11),
      false,
      config.getSeed(),
      ImmutableList.of(),
      config.getShouldTickTime(),
      null
    );

    this.config = config;
    this.flat = config.getFlat().orElse(super.isFlat());
  }

  public SandboxWorldConfig getConfig() {
    return this.config;
  }

  @Override
  public long getSeed() {
    return ((SandboxWorldProperties) this.properties).config.getSeed();
  }

  public boolean getShouldSaveWorld() {
    return ((SandboxWorldProperties) this.properties).config.getShouldSaveWorld();
  }

  @Override
  public boolean isFlat() {
    return this.flat;
  }

  @Override
  public void save(@Nullable ProgressListener progressListener, boolean flush, boolean enabled) {
  if (!this.getShouldSaveWorld() || !flush) {
    super.save(progressListener, flush, enabled);
  }
  }
  
  public interface Constructor {
    SandboxWorld createWorld(MinecraftServer server, RegistryKey<World> registryKey, SandboxWorldConfig config);
  }
}
