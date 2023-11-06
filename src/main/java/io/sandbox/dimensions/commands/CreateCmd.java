package io.sandbox.dimensions.commands;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.sandbox.dimensions.commands.autoComplete.StringListAutoComplete;
import io.sandbox.dimensions.dimension.DimensionManager;
import io.sandbox.dimensions.dimension.DimensionSave;
import io.sandbox.dimensions.mixin.MinecraftServerAccessor;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.resource.Resource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.random.RandomSequencesState;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.World;
import net.minecraft.world.border.WorldBorderListener.WorldBorderSyncer;
import net.minecraft.world.dimension.DimensionOptions;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.level.UnmodifiableLevelProperties;
import net.minecraft.world.level.storage.LevelStorage.Session;

public class CreateCmd {
  public static LiteralArgumentBuilder<ServerCommandSource> register() {
    return CommandManager.literal("create").then(
      CommandManager
      .argument("datapack", StringArgumentType.word())
      .suggests(new StringListAutoComplete((context) -> {
        Session session = ((MinecraftServerAccessor)(context.getSource().getServer())).getSession();
        Path datapackPath = session.getDirectory(WorldSavePath.DATAPACKS);
        File[] folderList = datapackPath.toFile().listFiles((dir, name) -> dir.isDirectory());
        List<String> datapackDirectoryList = new ArrayList<>();

        for (File datapackDir : folderList) {
          datapackDirectoryList.add(datapackDir.getName());
        }

        return datapackDirectoryList;
      }))
      .then(
        CommandManager
        .argument("namespace", StringArgumentType.word())
        .suggests(new StringListAutoComplete((context) -> {
          // Get previous argument to build the path to namespace inside datapack
          String datapackName = context.getArgument("datapack", String.class);
          Session session = ((MinecraftServerAccessor)(context.getSource().getServer())).getSession();
          Path datapackPath = session.getDirectory(WorldSavePath.DATAPACKS);
          File datapackDataDirectory = Paths.get(datapackPath.toString(), datapackName, "data").toFile();
          List<String> namespaceFolderList = new ArrayList<>();

          if (datapackDataDirectory.exists()) {
            File[] folderList = datapackDataDirectory.listFiles((dir, name) -> dir.isDirectory());
        
            for (File datapackDir : folderList) {
              // get the folder name
              namespaceFolderList.add(datapackDir.getName());
            }
          }

          return namespaceFolderList;
        }))
        .then(
          // This is the create command so dimensionName needs to be new for this namespace
          // can't auto-complete a new name
          // TODO: set validation for existing dimesionNames
          CommandManager
          .argument("dimensionName", StringArgumentType.word())
          .then(
            CommandManager
            .argument("dimensionType", IdentifierArgumentType.identifier())
            .suggests(new StringListAutoComplete((context) -> {
              Set<RegistryKey<DimensionType>> dimensionTypes = context.getSource().getServer()
                .getRegistryManager().get(RegistryKeys.DIMENSION_TYPE).getKeys();
              List<String> dimensionTypeList = new ArrayList<>();

              for (RegistryKey<DimensionType> dimensionType : dimensionTypes) {
                dimensionTypeList.add(dimensionType.getValue().toString());    
              }

              return dimensionTypeList;
            })) 
            .executes(ctx -> createDimension(
              StringArgumentType.getString(ctx, "datapack"),
              StringArgumentType.getString(ctx, "namespace"),
              StringArgumentType.getString(ctx, "dimensionName"),
              IdentifierArgumentType.getIdentifier(ctx, "dimensionType"),
              ctx.getSource()
            ))
          )
        )
      )
    ).executes(context -> {
      // TODO: add fallback messaging
      System.out.println("Fallback????");
      return 1;
    });
  }

  private static int createDimension(String datapackName, String namespace, String dimensionName, Identifier dimensionType, ServerCommandSource source) {
    MinecraftServer server = source.getServer();
    MinecraftServerAccessor serverAccess = (MinecraftServerAccessor)(server);
    Session session = serverAccess.getSession();
    String datapackPath = session.getDirectory(WorldSavePath.DATAPACKS).toString();
    Identifier defaultDimensionType = DimensionManager.getDefaultConfig("dimension"); // Filename matches dimension.json
    Identifier defaultWorld = DimensionManager.getDefaultConfig("world"); // Filename matches world.json
    System.out.println("defaults: " + defaultDimensionType + " : " + defaultWorld);
    if (defaultDimensionType != null && defaultWorld != null) {
      Optional<Resource> dimTypeResourceOptional = server.getResourceManager().getResource(defaultDimensionType);
      Optional<Resource> worldResourceOptional = server.getResourceManager().getResource(defaultWorld);
      
      if (dimTypeResourceOptional.isPresent()  && worldResourceOptional.isPresent()) {
        Resource dimensionTypeResource = dimTypeResourceOptional.get();
        Resource worldResource = worldResourceOptional.get();
        Identifier dimensionIdentifier = new Identifier(namespace, dimensionName);
        try {
          InputStream dimensionTypeIntputStream = dimensionTypeResource.getInputStream();
          InputStream worldInputStream = worldResource.getInputStream();
          Path dimensionConfigPath = Paths.get(datapackPath, datapackName, "data", namespace, "dimension", dimensionName + ".json");
          Path worldSavePath = Paths.get(datapackPath, datapackName, "data", namespace, DimensionSave.WORLD_SAVE_FOLDER, dimensionName + ".zip");
          System.out.println("WorldPath: " + worldSavePath.toString());

          // Create new dimension.json file for the new dimension
          try {
            Files.copy(dimensionTypeIntputStream, dimensionConfigPath, StandardCopyOption.REPLACE_EXISTING);
            dimensionTypeIntputStream.close();
          } catch (IOException e) {
            e.printStackTrace();
          }
          // copy over a default world save to use as default
          try {
            Files.copy(worldInputStream, worldSavePath, StandardCopyOption.REPLACE_EXISTING);
            worldInputStream.close();
            // worldInputStream.
          } catch (IOException e) {
            e.printStackTrace();
          }

          // Process the save zip
          // DimensionSave dimensionSave = DimensionSave.getDimensionState(dimensionWorld);
          String dimensionIdString = dimensionIdentifier.toString();
          System.out.println("Loading World File: " + dimensionIdString);
          DimensionManager.addDimensionPackName(dimensionIdString, datapackName);
          DimensionSave.loadDimensionFile(
            dimensionIdString,
            server
          );

          // server.stop(false);

          // ((MinecraftServerAccessor)(server)).invokeLoadWorld();

          RegistryKey<World> registryKey = RegistryKey.of(RegistryKeys.WORLD, dimensionIdentifier);
          SaveProperties saveProperties = server.getSaveProperties();
          UnmodifiableLevelProperties unmodifiableLevelProperties = new UnmodifiableLevelProperties(saveProperties, saveProperties.getMainWorldProperties());

          Registry<DimensionOptions> dimensionOptions = server.getRegistryManager().get(RegistryKeys.DIMENSION);
          DimensionOptions dimensionOption = dimensionOptions.get(DimensionOptions.OVERWORLD);
          // List<Spawner> list = ImmutableList.of(new PhantomSpawner(), new PatrolSpawner(), new CatSpawner(), new ZombieSiegeManager(), new WanderingTraderManager(serverWorldProperties));
          
          ServerWorld dimensionWorld = new ServerWorld(
            server,
            serverAccess.getWorkerExecutor(),
            session,
            unmodifiableLevelProperties,
            registryKey,
            dimensionOption,
            serverAccess.getWorldGenerationProgressListenerFactory().create(11),
            false,
            1234L,
            ImmutableList.of(),
            true,
            (RandomSequencesState)null
          );
          server.getWorld(World.OVERWORLD).getWorldBorder().addListener(new WorldBorderSyncer(dimensionWorld.getWorldBorder()));
          // worldBorder.addListener(new WorldBorderSyncer(serverWorld.getWorldBorder()));
          serverAccess.getWorlds().put(registryKey, dimensionWorld);
          DimensionSave dimensionSave = DimensionSave.getDimensionState(dimensionWorld);
          DimensionManager.addDimensionSave(dimensionIdString, dimensionSave);
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }

    // Set<RegistryKey<DimensionType>> tests = source.getServer().getRegistryManager().get(RegistryKeys.DIMENSION_TYPE).getKeys();
    // for (RegistryKey<DimensionType> test : tests) {
    //   System.out.println("TEST: " + test.getValue().toString());    
    // }
    // System.out.println("Create: " + dimension + " : " + namespace);
    source.sendFeedback(() -> {
      return Text.literal("Create new Dimension");
    }, false);
    return 1;
  }
}
