package io.sandbox.dimensions.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;

public class CommandInit {
  public static void init () {
    CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
			LiteralArgumentBuilder.<ServerCommandSource>literal("dimension")
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
				.then(RestoreCommand.register())
        .then(DownloadDimension.register())));
  }
}
