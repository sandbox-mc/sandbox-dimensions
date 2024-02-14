package io.sandboxmc.chunkGenerators;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import org.jetbrains.annotations.Nullable;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.block.BlockState;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.ChunkRegion;
import net.minecraft.world.HeightLimitView;
import net.minecraft.world.Heightmap.Type;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.biome.BiomeKeys;
import net.minecraft.world.biome.source.BiomeAccess;
import net.minecraft.world.biome.source.BiomeSource;
import net.minecraft.world.biome.source.FixedBiomeSource;
import net.minecraft.world.chunk.Chunk;
import net.minecraft.world.gen.GenerationStep.Carver;
import net.minecraft.world.gen.StructureAccessor;
import net.minecraft.world.gen.chunk.Blender;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.chunk.VerticalBlockSample;
import net.minecraft.world.gen.noise.NoiseConfig;
import net.minecraft.world.gen.structure.Structure;

public class EmptyChunkGenerator extends ChunkGenerator {

  private RegistryEntry<Biome> biome = null;
  private static final VerticalBlockSample EMPTY_SAMPLE = new VerticalBlockSample(0, new BlockState[0]);
  // private Object biome;
  public static final Codec<EmptyChunkGenerator> CODEC = RecordCodecBuilder.create(instance -> {
    return instance.group(
      Biome.REGISTRY_CODEC.stable().fieldOf("biome").forGetter(g -> g.biome)
    ).apply(instance, instance.stable(EmptyChunkGenerator::new));
  });

  public EmptyChunkGenerator(BiomeSource biomeSource) {
    super(biomeSource);
    //TODO Auto-generated constructor stub
  }

  public EmptyChunkGenerator(RegistryEntry<Biome> biome) {
    super(new FixedBiomeSource(biome));
    this.biome = biome;
  }

  public EmptyChunkGenerator(Registry<Biome> biomeRegistry) {
    this(biomeRegistry, BiomeKeys.THE_VOID);
  }

  public EmptyChunkGenerator(Registry<Biome> biomeRegistry, RegistryKey<Biome> biome) {
    this(biomeRegistry.getEntry(biome).get());
  }

  @Override
  public void buildSurface(ChunkRegion region, StructureAccessor structures, NoiseConfig noiseConfig, Chunk chunk) {}

  @Override
  public void carve(
    ChunkRegion chunkRegion,
    long seed,
    NoiseConfig noiseConfig,
    BiomeAccess biomeAccess,
    StructureAccessor structureAccessor,
    Chunk chunk,
    Carver carverStep
  ) {}

  @Override
  protected Codec<? extends ChunkGenerator> getCodec() {
    return CODEC;
  }

  @Override
  public VerticalBlockSample getColumnSample(int x, int z, HeightLimitView world, NoiseConfig noiseConfig) {
    return EMPTY_SAMPLE;
  }

  @Override
  public void getDebugHudText(List<String> text, NoiseConfig noiseConfig, BlockPos pos) {}

  @Override
  public int getHeight(int x, int z, Type heightmap, HeightLimitView world, NoiseConfig noiseConfig) {
    return 0;
  }

  @Override
  public int getMinimumY() {
    return -64;
  }

  @Override
  public int getSeaLevel() {
    return 64;
  }

  @Override
  public int getWorldHeight() {
    return 320;
  }

  @Nullable
  @Override
  public Pair<BlockPos, RegistryEntry<Structure>> locateStructure(
    ServerWorld world,
    RegistryEntryList<Structure> structures,
    BlockPos center,
    int radius,
    boolean skipReferencedStructures
  ) {
    return null;
  }

  @Override
  public void populateEntities(ChunkRegion region) {}

  @Override
  public CompletableFuture<Chunk> populateNoise(
    Executor executor,
    Blender blender,
    NoiseConfig noiseConfig,
    StructureAccessor structureAccessor,
    Chunk chunk
  ) {
    return CompletableFuture.completedFuture(chunk);
  }
}
