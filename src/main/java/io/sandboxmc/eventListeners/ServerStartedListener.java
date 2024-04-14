package io.sandboxmc.eventListeners;

import io.sandboxmc.datapacks.DatapackManager;
import io.sandboxmc.dimension.DimensionManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStarted;
import net.minecraft.server.MinecraftServer;

public class ServerStartedListener implements ServerStarted {
  @Override
  public void onServerStarted(MinecraftServer server) {
    // DimensionManager needs access to the server first
    DimensionManager.onServerStarted(server);
    DatapackManager.onServerStarted(server);
    DimensionManager.processSandboxDimensionFiles(server);
  }
}
