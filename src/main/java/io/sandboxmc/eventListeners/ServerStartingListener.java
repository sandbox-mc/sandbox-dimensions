package io.sandboxmc.eventListeners;

import io.sandboxmc.Config;
import io.sandboxmc.Plunger;
import io.sandboxmc.json.JsonContainer;
import io.sandboxmc.web.Server;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.ServerStarting;
import net.minecraft.server.MinecraftServer;

public class ServerStartingListener implements ServerStarting {
  @Override
  public void onServerStarting(MinecraftServer server) {
    Config.setConfigs(server);
    JsonContainer jsonConfig = new JsonContainer(server.getFile("json_config_example.json"));
    jsonConfig.loadDataFromFile();
    Plunger.debug("CREATED JSON AS:\n" + jsonConfig.toString());
    JsonContainer.usageExample(server.getFile("json_config_output_example.json").toPath());
    Server.authOnBoot(server);
  }
}
