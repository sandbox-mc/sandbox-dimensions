package io.sandboxmc.mixin;

import java.io.IOException;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.sandboxmc.Web;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

@Mixin(ServerPlayerEntity.class)
public abstract class ServerPlayerEntityMixin extends Entity {

  public ServerPlayerEntityMixin(EntityType<?> type, World world) {
    super(type, world);
    throw new IllegalStateException("ServerPlayerEntityMixin's dummy constructor called!");
  }

  @Inject(method = "onDisconnect", at = @At("HEAD"), cancellable = true)
  public void logoutOnDisconnect(CallbackInfo cbir) {
    ServerCommandSource source = this.getCommandSource();

    // Same as the code in the WebLogout command, but it doesn't have any of the feedback messages
    if (Web.getBearerToken(source) == null) {
      return;
    }

    Web web = new Web(source, "/clients/auth/logout", true);
    web.setDeleteBody();

    try {
      web.getString(); // we don't even need to do anything with this...
      Web.removeBearerToken(source);
    } catch (IOException e) {
      // I don't think we care?
    } finally {
      web.closeReaders();
    }
  }
}
