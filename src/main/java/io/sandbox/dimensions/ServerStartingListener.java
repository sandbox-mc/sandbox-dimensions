package io.sandbox.dimensions;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStarting;
import net.minecraft.server.MinecraftServer;

public class ServerStartingListener implements ServerStarting {
  @Override
  public void onServerStarting(MinecraftServer server) {
    System.out.println("Hit the server");
  }
}
