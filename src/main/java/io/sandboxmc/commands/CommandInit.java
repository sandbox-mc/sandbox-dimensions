package io.sandboxmc.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;

import io.sandboxmc.commands.web.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;

public class CommandInit {
  public static void init () {
    CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			CommandNode<ServerCommandSource> baseCommandNode = dispatcher.register(
				LiteralArgumentBuilder.<ServerCommandSource>literal("sandboxmc")
					.requires((ctx) -> {
						try {
							// This is command auth, should look into this at some point
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
					.then(CreateCmd.register())
					.then(Rule.register())
					.then(InstallDatapack.register())
					.then(JoinDimension.register())
					.then(LeaveDimension.register())
					.then(ListDimensions.register())
					.then(RestoreDimension.register())
					.then(SaveDimension.register())
					.then(SetSpawnDimension.register())
					// Web related commands
					.then(DownloadDimensionCmd.register())
					.then(UploadDimensionCmd.register())
					.then(ClientAuthCmd.register("auth"))
					.then(ClientAuthCmd.register("authenticate"))
					.then(ClientLogoutCmd.register())
				);
				dispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>literal("sbmc").redirect(baseCommandNode));
		});
  }
}
