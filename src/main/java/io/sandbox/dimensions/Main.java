package io.sandbox.dimensions;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import io.sandbox.dimensions.commands.DownloadDimension;
import io.sandbox.dimensions.commands.RestoreDimension;

public class Main implements ModInitializer {
	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
    public static final Logger LOGGER = LoggerFactory.getLogger("Sandbox Dimensions");

	@Override
	public void onInitialize() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
			LiteralArgumentBuilder.<ServerCommandSource>literal("dim")
				.requires((ctx) -> {
					try {
						// return ((config.get().playerWhitelist.contains(ctx.getEntityOrThrow().getEntityName()) ||
						// 				ctx.hasPermissionLevel(config.get().permissionLevel)) &&
						// 				!config.get().playerBlacklist.contains(ctx.getEntityOrThrow().getEntityName())) ||
						// 				(ctx.getServer().isSingleplayer() &&
						// 								config.get().alwaysSingleplayerAllowed);
					} catch (Exception ignored) { //Command was called from server console.
						return true;
					}
					return true;
				})
				.then(RestoreDimension.register())
				.then(DownloadDimension.register())));
	}
}