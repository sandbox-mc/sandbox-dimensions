package io.sandbox.dimensions.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.sandbox.dimensions.dimension.DimensionSave;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {
  @Inject(method = "dropInventory", at = @At("HEAD"), cancellable = true)
  public void safeInv(CallbackInfo ci) {
    ServerPlayerEntity serverPlayer = (ServerPlayerEntity) (Object) this;
    ServerWorld dimension = serverPlayer.getServerWorld();
    DimensionSave dimensionSave = DimensionSave.getDimensionState(dimension);
    Boolean keepInventory = dimensionSave.getRule(DimensionSave.KEEP_INVENTORY_ON_DEATH);
    
    if (keepInventory) {
      // Cancel this method to prevent the .dropAll() method from being called
      ci.cancel();
    }
  }
}
