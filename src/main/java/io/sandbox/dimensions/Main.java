package io.sandbox.dimensions;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.sandbox.dimensions.commands.CommandInit;
import io.sandbox.dimensions.dimension.DimensionManager;

public class Main implements ModInitializer {
	public static final String modId = "sandbox-dimensions";
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger("Sandbox Dimensions");

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		ServerLifecycleEvents.SERVER_STARTING.register(new ServerStartingListener());

		// Initialize Commands
		CommandInit.init();
		DimensionManager.init();
	}

	public static Identifier id(String name) {
		return new Identifier(Main.modId, name);
	}
}