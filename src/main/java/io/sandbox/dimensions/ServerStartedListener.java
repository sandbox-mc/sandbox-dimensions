package io.sandbox.dimensions;

import io.sandbox.dimensions.dimension.DimensionManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStarted;
import net.minecraft.server.MinecraftServer;

public class ServerStartedListener implements ServerStarted {
  @Override
  public void onServerStarted(MinecraftServer server) {
    DimensionManager.processSandboxDimensionFiles(server);
  }
}
