package io.sandboxmc.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.tree.CommandNode;

import io.sandboxmc.commands.dimension.CreateDimension;
import io.sandboxmc.commands.dimension.DeleteDimension;
import io.sandboxmc.commands.dimension.ListDimensions;
import io.sandboxmc.commands.datapack.AddDimension;
import io.sandboxmc.commands.datapack.CreateDatapack;
import io.sandboxmc.commands.datapack.InstallDatapack;
import io.sandboxmc.commands.datapack.SetDatapackDescription;
import io.sandboxmc.commands.datapack.SetDatapackFormat;
import io.sandboxmc.commands.dimension.CopyDimension;
import io.sandboxmc.commands.web.*;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.CommandManager;
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
					.then(
						CommandManager.literal("datapack")
						.then(AddDimension.register())
						.then(CreateDatapack.register())
						.then(InstallDatapack.register())
						.then(SetDatapackDescription.register())
						.then(SetDatapackFormat.register())
					)
					.then(
						// Sub command dimension
						CommandManager.literal("dimension")
						.then(CreateDimension.register())
						.then(DeleteDimension.register())
						.then(ListDimensions.register())
						.then(CopyDimension.register())
					)
					.then(Rule.register())
					.then(JoinDimension.register())
					.then(LeaveDimension.register())
					.then(RestoreDimension.register())
					.then(SetSpawnDimension.register())
					// Web related commands
					.then(DownloadCmd.register())
					.then(UploadCmd.register())
					.then(ClientAuthCmd.register("auth"))
					.then(ClientAuthCmd.register("authenticate"))
					.then(ClientLogoutCmd.register())
					.then(ServerCmd.register("server"))
				);
				dispatcher.register(LiteralArgumentBuilder.<ServerCommandSource>literal("sbmc").redirect(baseCommandNode));
		});
  }
}
