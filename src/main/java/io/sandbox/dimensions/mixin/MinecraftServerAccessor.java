package io.sandbox.dimensions.mixin;

import java.util.Map;
import java.util.concurrent.Executor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.registry.RegistryKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.WorldGenerationProgressListenerFactory;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import net.minecraft.world.level.storage.LevelStorage;

@Mixin(MinecraftServer.class)
public interface MinecraftServerAccessor {
	@Accessor
	LevelStorage.Session getSession();

	@Accessor
	WorldGenerationProgressListenerFactory getWorldGenerationProgressListenerFactory();

	@Accessor
	Executor getWorkerExecutor();

	@Accessor
	Map<RegistryKey<World>, ServerWorld> getWorlds();

	@Invoker("loadWorld")
	public void invokeLoadWorld();
}
