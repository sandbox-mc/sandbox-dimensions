package io.sandboxmc.commands.dimension;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import io.sandboxmc.dimension.DimensionManager;
import io.sandboxmc.commands.autoComplete.StringListAutoComplete;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.world.dimension.DimensionOptions;

public class CreateDimension {
  public static LiteralArgumentBuilder<ServerCommandSource> register() {
    return CommandManager.literal("create").then(
      CommandManager
      .argument("namespace", StringArgumentType.word())
      // TODO:BRENT not sure how to get a suggestion for these without a namespace
      // .suggests(new StringListAutoComplete(getNamespaceAutoCompleteOptions()))
      .then(
        // This is the create command so dimensionName needs to be new for this namespace
        // can't auto-complete a new name
        // TODO:BRENT set validation for existing dimesionNames
        CommandManager
        .argument("dimensionName", StringArgumentType.word())
        .then(
          CommandManager
          .argument("dimension", IdentifierArgumentType.identifier())
          .suggests(new StringListAutoComplete(getDimensionAutoCompleteOptions()))
          .executes(context -> createDimension(context))
        ).executes(context -> createDimension(context))
      )
    ).executes(context -> {
      // No arguments given, do nothing.
      return 0;
    });
  }

  // private static Function<CommandContext<ServerCommandSource>, List<String>> getDatapackAutoCompleteOptions() {
  //   return (context) -> {
  //     Session session = ((MinecraftServerAccessor)(context.getSource().getServer())).getSession();
  //     Path datapackPath = session.getDirectory(WorldSavePath.DATAPACKS);
  //     File[] folderList = datapackPath.toFile().listFiles((dir, name) -> dir.isDirectory());
  //     List<String> datapackNames = new ArrayList<>();
  //     for (File file : folderList) {
  //       String fileName = file.getName();
  //       // We want to ignore .zip files for now
  //       if(!fileName.endsWith(".zip")) {
  //         datapackNames.add(fileName);
  //       }
  //     }

  //     return datapackNames;
  //   };
  // }

  private static Function<CommandContext<ServerCommandSource>, List<String>> getDimensionAutoCompleteOptions() {
    return (context) -> {
      Set<RegistryKey<DimensionOptions>> dimensionTypes = context.getSource().getServer()
        .getRegistryManager().get(RegistryKeys.DIMENSION).getKeys();
      List<String> dimensionTypeList = new ArrayList<>();

      for (RegistryKey<DimensionOptions> dimensionType : dimensionTypes) {
        dimensionTypeList.add(dimensionType.getValue().toString());    
      }

      return dimensionTypeList;
    };
  }

  // private static Function<CommandContext<ServerCommandSource>, List<String>> getNamespaceAutoCompleteOptions() {
  //   return (context) -> {
  //     // Get previous argument to build the path to namespace inside datapack
  //     String datapackName = context.getArgument("datapack", String.class);
  //     Session session = ((MinecraftServerAccessor)(context.getSource().getServer())).getSession();
  //     Path datapackPath = session.getDirectory(WorldSavePath.DATAPACKS);
  //     File datapackDataDirectory = Paths.get(datapackPath.toString(), datapackName, "data").toFile();
  //     List<String> namespaceFolderList = new ArrayList<>();

  //     if (datapackDataDirectory.exists()) {
  //       File[] folderList = datapackDataDirectory.listFiles((dir, name) -> dir.isDirectory());
    
  //       for (File datapackDir : folderList) {
  //         // get the folder name
  //         namespaceFolderList.add(datapackDir.getName());
  //       }
  //     }

  //     return namespaceFolderList;
  //   };
  // }

  private static int createDimension(CommandContext<ServerCommandSource> context) {
    // String datapackName = StringArgumentType.getString(context, "datapack");
    String namespace = StringArgumentType.getString(context, "namespace");
    // namespace = "sandboxhidden"; // TODO:BRENT remove this override
    String dimensionName = StringArgumentType.getString(context, "dimensionName");
    Identifier dimensionOptions = IdentifierArgumentType.getIdentifier(context, "dimension");
    Identifier dimensionIdentifier = new Identifier(namespace, dimensionName);
    ServerCommandSource source = context.getSource();
    MinecraftServer server = source.getServer();

    // Create the dimension
    DimensionManager.createDimensionWorld(server, dimensionIdentifier, dimensionOptions);

    // Datapack datapack = DatapackManager.getDatapack(datapackName);
    // if (datapack == null) {
    //   datapack = DatapackManager.createDatapack(datapackName);
    //   // if it doesn't exist we need to create the default files for it
    //   // This is actually for server restarts
    //   datapack.initializeDatapack(server);
    // }

    // Identifier defaultDimensionType = DimensionManager.getDefaultConfig("dimension"); // Filename matches dimension.json
    // Identifier defaultWorld = DimensionManager.getDefaultConfig("world"); // Filename matches world.json
    // if (defaultDimensionType != null && defaultWorld != null) {
    //   Optional<Resource> dimTypeResourceOptional = server.getResourceManager().getResource(defaultDimensionType);
    //   Optional<Resource> worldResourceOptional = server.getResourceManager().getResource(defaultWorld);
      
    //   if (dimTypeResourceOptional.isPresent()  && worldResourceOptional.isPresent()) {
    //     Resource dimensionTypeResource = dimTypeResourceOptional.get();
    //     Resource worldResource = worldResourceOptional.get();
    //     // Identifier dimensionIdentifier = new Identifier(namespace, dimensionName);
    //     try {
    //       InputStream dimensionTypeIntputStream = dimensionTypeResource.getInputStream();
    //       InputStream worldInputStream = worldResource.getInputStream();

    //       // Create new dimension.json file for the new dimension
    //       datapack.addDimensionFile(namespace, dimensionName, dimensionTypeIntputStream);

    //       // Copy over a default world save to use as default
    //       datapack.addWorldSaveFile(namespace, dimensionName, worldInputStream);

    //       // Process the save zip
    //       String dimensionIdString = dimensionIdentifier.toString();
    //       System.out.println("Loading World File: " + dimensionIdString);
    //       // loadDimensionFile requires a reference to the packName
    //       // DimensionManager.addDimensionToPacknameMap(dimensionIdString, datapackName);
    //       // DimensionSave.loadDimensionFile(
    //       //   dimensionIdentifier,
    //       //   server
    //       // );

    //       // Create the dimension
    //       DimensionManager.createDimensionWorld(server, dimensionIdentifier);
    //     } catch (IOException e) {
    //       e.printStackTrace();
    //     }
    //   }
    // }

    source.sendFeedback(() -> {
      return Text.literal("Created new Dimension: " + dimensionName);
    }, false);
    return 1;
  }
}
