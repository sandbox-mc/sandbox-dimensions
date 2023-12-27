package io.sandboxmc.commands;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.sandboxmc.Web;
import io.sandboxmc.dimension.DimensionManager;
import io.sandboxmc.mixin.MinecraftServerAccessor;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.world.level.storage.LevelStorage.Session;

public class UploadDimension implements Runnable {
  public static LiteralArgumentBuilder<ServerCommandSource> register() {
    return CommandManager.literal("upload")
      .then(
        CommandManager.argument("dimension-file", StringArgumentType.word())
        // .suggests(existing dimension files)
        .executes(context -> performUploadCmd(context))
      )
      .executes(context -> {
        printMessage(context.getSource(), "No dimension file specified");
        return 0;
      });
  }

  private static int performUploadCmd(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    // // EXAMPLE CODE: for building the tmpZip file for upload
    // // Step 1) Create a world
    // // Step 2) Save that world
    // // Step 3) this command...
    //
    // ServerWorld dimension = DimensionArgumentType.getDimensionArgument(context, "dimension");
    // Identifier dimensionId = dimension.getRegistryKey().getValue();
    // String datapackName = DatapackManager.getDatapackName(dimensionId);
    // Datapack datapack = DatapackManager.getDatapack(datapackName);
    // datapack.zipWorldfilesToDatapack(dimension);

    // Path tmpZipPath; // This is the path to the .zip file
    // try {
    //   tmpZipPath = datapack.createTmpZip();
    // } catch(IOException ex) {
    //   ex.printStackTrace();
    // }

    // // Should be able to run datapack.deleteTmpZipFile(); when you're done with the tmpFile

    ServerCommandSource source = context.getSource();
    String dimensionFileName = StringArgumentType.getString(context, "dimension-file");
    
    Runnable uploadThread = new UploadDimension(source, dimensionFileName);
    new Thread(uploadThread).start();

    return 1;
  }

  private ServerCommandSource source;
  private Session session;
  private String dimensionFileName;

  public UploadDimension(ServerCommandSource commandSource, String theDimensionFileName) {
    source = commandSource;
    session = ((MinecraftServerAccessor)commandSource.getServer()).getSession();
    dimensionFileName = theDimensionFileName;
  }

  public void run() {
    Web web = new Web(source, "/dimensions/upload", true);
    String filePath = defaultFilePath(session, dimensionFileName + ".zip");
    File file = new File(filePath);
    if (!file.exists()) {
      printMessage(source, "Dimension not found at " + filePath);
    }

    web.setFormField("dimension", file);
    web.finalizeFormAsPostBody();

    try {
      web.executeRequest();
      if (web.getStatusCode() == 200) {
        printMessage(source, "Dimension successfully uploaded!");
      } else {
        web.printJsonMessages("Upload failed!");
      }
    } catch (IOException e) {
      printMessage(source, "Something went wrong, failed to upload dimension.");
    } finally {
      web.closeReaders();
    }
  }

  private static String defaultFilePath(Session session, String fileName) {
    Path storageFolder = DimensionManager.getStorageFolder(session);
    return Paths.get(storageFolder.toString(), fileName).toString();
  }

  private static void printMessage(ServerCommandSource source, String feedbackText) {
    source.sendFeedback(() -> {
      return Text.literal(feedbackText);
    }, false);
  }
}
