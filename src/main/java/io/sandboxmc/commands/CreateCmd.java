package io.sandboxmc.commands;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import io.sandboxmc.datapacks.Datapack;
import io.sandboxmc.datapacks.DatapackManager;
import io.sandboxmc.dimension.DimensionManager;
import io.sandboxmc.dimension.DimensionSave;
import io.sandboxmc.mixin.MinecraftServerAccessor;
import io.sandboxmc.commands.autoComplete.StringListAutoComplete;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.resource.Resource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.dimension.DimensionType;
import net.minecraft.world.level.storage.LevelStorage.Session;

public class CreateCmd {
  public static LiteralArgumentBuilder<ServerCommandSource> register() {
    return CommandManager.literal("create").then(
      CommandManager
      .argument("datapack", StringArgumentType.word())
      .suggests(new StringListAutoComplete(getDatapackAutoCompleteOptions()))
      .then(
        CommandManager
        .argument("namespace", StringArgumentType.word())
        .suggests(new StringListAutoComplete(getNamespaceAutoCompleteOptions()))
        .then(
          // This is the create command so dimensionName needs to be new for this namespace
          // can't auto-complete a new name
          // TODO: set validation for existing dimesionNames
          CommandManager
          .argument("dimensionName", StringArgumentType.word())
          .then(
            CommandManager
            .argument("dimensionType", IdentifierArgumentType.identifier())
            .suggests(new StringListAutoComplete(getDimensionTypeAutoCompleteOptions())) 
            .executes(context -> createDimension(context))
          )
        )
      )
    ).executes(context -> {
      // TODO: add fallback messaging
      System.out.println("Fallback????");
      return 1;
    });
  }

  private static Function<CommandContext<ServerCommandSource>, List<String>> getDatapackAutoCompleteOptions() {
    return (context) -> {
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
    };
  }

  private static Function<CommandContext<ServerCommandSource>, List<String>> getDimensionTypeAutoCompleteOptions() {
    return (context) -> {
      Set<RegistryKey<DimensionType>> dimensionTypes = context.getSource().getServer()
        .getRegistryManager().get(RegistryKeys.DIMENSION_TYPE).getKeys();
      List<String> dimensionTypeList = new ArrayList<>();

      for (RegistryKey<DimensionType> dimensionType : dimensionTypes) {
        dimensionTypeList.add(dimensionType.getValue().toString());    
      }

      return dimensionTypeList;
    };
  }

  private static Function<CommandContext<ServerCommandSource>, List<String>> getNamespaceAutoCompleteOptions() {
    return (context) -> {
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
    };
  }

  private static int createDimension(CommandContext<ServerCommandSource> context) {
    String datapackName = StringArgumentType.getString(context, "datapack");
    String namespace = StringArgumentType.getString(context, "namespace");
    String dimensionName = StringArgumentType.getString(context, "dimensionName");
    // Identifier dimensionType = IdentifierArgumentType.getIdentifier(context, "dimensionType");
    ServerCommandSource source = context.getSource();
    MinecraftServer server = source.getServer();

    Datapack datapack = DatapackManager.geDatapack(datapackName);
    if (datapack == null) {
      datapack = DatapackManager.createDatapack(datapackName);
      // if it doesn't exist we need to create the default files for it
      // This is actually for server restarts
      datapack.saveDatapack();
    }

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

          // Create new dimension.json file for the new dimension
          datapack.addDimensionFile(namespace, dimensionName, dimensionTypeIntputStream);

          // Copy over a default world save to use as default
          datapack.addWorldSaveFile(namespace, dimensionName, worldInputStream);

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
