package io.sandbox.dimensions;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.util.Identifier;
import io.sandbox.dimensions.commands.CommandInit;
import io.sandbox.dimensions.configs.PlayerRespawnConfig;
import io.sandbox.dimensions.dimension.DimensionManager;
import io.sandbox.lib.SandboxLogger;

public class Main implements ModInitializer {
  public static final String modId = "sandbox-dimensions";
  // This logger is used to write text to the console and the log file.
  // It is considered best practice to use your mod id as the logger's name.
  // That way, it's clear which mod wrote info, warnings, and errors.
  public static final SandboxLogger LOGGER = new SandboxLogger("SandboxDimensions");

  @Override
  public void onInitialize() {
    // This code runs as soon as Minecraft is in a mod-load-ready state.
    // However, some things (like resources) may still be uninitialized.
    // Proceed with mild caution.

    ServerLifecycleEvents.SERVER_STARTED.register(new ServerStartedListener());

    // Initialize Commands
    CommandInit.init();
    DimensionManager.init();
    PlayerRespawnConfig.initRespawnListener();
  }

  public static Identifier id(String name) {
    return new Identifier(Main.modId, name);
  }
}