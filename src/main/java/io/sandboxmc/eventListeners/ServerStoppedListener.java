package io.sandboxmc.eventListeners;

import io.sandboxmc.dimension.DimensionManager;
import io.sandboxmc.web.Server;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStopped;
import net.minecraft.server.MinecraftServer;

public class ServerStoppedListener implements ServerStopped {
  @Override
  public void onServerStopped(MinecraftServer server) {
    DimensionManager.unload();
    Server.handleAuthForShutdown(server);
  }
}
