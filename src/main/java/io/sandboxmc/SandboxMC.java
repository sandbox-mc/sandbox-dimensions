package io.sandboxmc;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.util.Identifier;
import io.sandboxmc.configs.PlayerRespawnConfig;
import io.sandboxmc.dimension.DimensionManager;
import io.sandboxmc.eventListeners.ServerStartedListener;
import io.sandboxmc.eventListeners.ServerStartingListener;
import io.sandboxmc.eventListeners.ServerStoppedListener;

import io.sandboxmc.chunkGenerators.ChunkGeneratorManager;
import io.sandboxmc.commands.CommandInit;

public class SandboxMC implements ModInitializer {
  public static final String MOD_ID = "sandboxmc";

  @Override
  public void onInitialize() {
    // This code runs as soon as Minecraft is in a mod-load-ready state.
    // However, some things (like resources) may still be uninitialized.
    // Proceed with mild caution.

    ServerLifecycleEvents.SERVER_STARTING.register(new ServerStartingListener());
    ServerLifecycleEvents.SERVER_STARTED.register(new ServerStartedListener());
    ServerLifecycleEvents.SERVER_STOPPED.register(new ServerStoppedListener());

    // Initialize Commands
    ChunkGeneratorManager.init();
    CommandInit.init();
    DimensionManager.init();
    PlayerRespawnConfig.initRespawnListener();
  }

  public static Identifier id(String name) {
    return new Identifier(SandboxMC.MOD_ID, name);
  }
}
