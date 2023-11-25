package io.sandboxmc.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;

public class CommandInit {
  public static void init () {
		// TODO: get my custom argument type registered
		// ArgumentTypeRegistry.registerArgumentType(new Identifier(Main.modId, "authcode"), AuthCodeArgumentType.class, ConstantArgumentSerializer.of(AuthCodeArgumentType::authCode));

    CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
			LiteralArgumentBuilder.<ServerCommandSource>literal("sb")
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
				.then(DownloadDimension.register())
				.then(JoinDimension.register())
				.then(LeaveDimension.register())
				.then(ListDimensions.register())
				.then(RestoreDimension.register())
				.then(SaveDimension.register())
				.then(SetSpawnDimension.register())
				.then(WebAuthenticate.register())
			)
		);
  }
}
