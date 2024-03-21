package io.sandboxmc.eventListeners;

import io.sandboxmc.Config;
import io.sandboxmc.json.JsonContainer;
import io.sandboxmc.web.Server;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStarting;
import net.minecraft.server.MinecraftServer;

public class ServerStartingListener implements ServerStarting {
  @Override
  public void onServerStarting(MinecraftServer server) {
    Config.setConfigs(server);
    JsonContainer.injestExample(server.getFile("json_injest_example.json"));
    JsonContainer.buildAndWriteExample(server.getFile("json_output_example.json"));
    Server.authOnBoot(server);
  }
}
