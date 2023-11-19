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
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.sandbox.dimensions.commands.autoComplete.StringListAutoComplete;
import io.sandbox.dimensions.datapacks.Datapack;
import io.sandbox.dimensions.dimension.DimensionManager;
import io.sandbox.dimensions.dimension.DimensionSave;
import io.sandbox.dimensions.mixin.MinecraftServerAccessor;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.resource.Resource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.dimension.DimensionType;
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
        List<String> datapackNames = new ArrayList<>();
        for (File file : folderList) {
          String fileName = file.getName();
          // We want to ignore .zip files for now
          if(!fileName.endsWith(".zip")) {
            datapackNames.add(fileName);
          }
        }

        return datapackNames;
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
    Path datapackPath = session.getDirectory(WorldSavePath.DATAPACKS);

    // Build list of files/dirs in /datapacks directory
    File[] folderList = datapackPath.toFile().listFiles((dir, name) -> dir.isDirectory());
    List<String> datapackNames = new ArrayList<>();
    for (File file : folderList) {
      datapackNames.add(file.getName());
    }

    // Check if datapackName is usable
    if (!datapackNames.contains(datapackName)) {
      // datapackName doesn't have file extensions, so if it's a .zip datapack it will get here.
      if (datapackNames.contains(datapackName + ".zip")) {
        source.sendFeedback(() -> {
          return Text.literal("Cannot add dimension to .zip datapack: " + dimensionName).formatted(Formatting.RED);
        }, false);
        return 0;
      } else {
        // Here were need to create the datapack...
        source.sendFeedback(() -> {
          return Text.literal("Creating new datapack: " + datapackName).formatted(Formatting.AQUA);
        }, false);
        // Create the Datapack
        Datapack datapack = new Datapack(datapackPath, datapackName);
        datapack.saveDatapack();
        // we need to make default folders for the dimension and the save
        datapack.addFolder(Paths.get("data", namespace, "dimension").toString());
        datapack.addFolder(Paths.get("data", namespace, DimensionSave.WORLD_SAVE_FOLDER).toString());
      }
    }

    String datapackPathString = datapackPath.toString();
    Identifier defaultDimensionType = DimensionManager.getDefaultConfig("dimension"); // Filename matches dimension.json
    Identifier defaultWorld = DimensionManager.getDefaultConfig("world"); // Filename matches world.json
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
          Path dimensionConfigPath = Paths.get(datapackPathString, datapackName, "data", namespace, "dimension", dimensionName + ".json");
          Path worldSavePath = Paths.get(datapackPathString, datapackName, "data", namespace, DimensionSave.WORLD_SAVE_FOLDER, dimensionName + ".zip");

          // Create new dimension.json file for the new dimension
          // This will allow for reloading this world
          try {
            Files.copy(dimensionTypeIntputStream, dimensionConfigPath, StandardCopyOption.REPLACE_EXISTING);
          } catch (IOException e) {
            e.printStackTrace();
          }

          // Copy over a default world save to use as default
          // This is the save file that will be used for the new dimension
          try {
            Files.copy(worldInputStream, worldSavePath, StandardCopyOption.REPLACE_EXISTING);
          } catch (IOException e) {
            e.printStackTrace();
          }

          // Process the save zip
          String dimensionIdString = dimensionIdentifier.toString();
          System.out.println("Loading World File: " + dimensionIdString);
          // loadDimensionFile requires a reference to the packName
          DimensionManager.addDimensionToPacknameMap(dimensionIdString, datapackName);
          DimensionSave.loadDimensionFile(
            dimensionIdString,
            server
          );

          // Create the dimension
          DimensionManager.createDimensionWorld(server, dimensionIdentifier);
        } catch (IOException e) {
          // TODO Auto-generated catch block
          e.printStackTrace();
        }
      }
    }

    source.sendFeedback(() -> {
      return Text.literal("Created new Dimension: " + dimensionName);
    }, false);
    return 1;
  }
}
