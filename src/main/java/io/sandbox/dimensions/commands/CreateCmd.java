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
import io.sandbox.dimensions.dimension.DimensionManager;
import io.sandbox.dimensions.mixin.MinecraftServerAccessor;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.resource.Resource;
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
    Session session = ((MinecraftServerAccessor)(source.getServer())).getSession();
    Path datapackPath = session.getDirectory(WorldSavePath.DATAPACKS);
    Identifier defaultType = DimensionManager.getDefaultConfig("dimension");
    if (defaultType != null) {
      Optional<Resource> resourceOptional = source.getServer().getResourceManager().getResource(defaultType);
      
      if (resourceOptional.isPresent()) {
        Resource resource = resourceOptional.get();
        try {
          InputStream intputStream = resource.getInputStream();
          Path filePath = Paths.get(datapackPath.toString(), datapackName, "data", namespace, "dimension", dimensionName + ".json");
          Files.copy(intputStream, filePath, StandardCopyOption.REPLACE_EXISTING);
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
