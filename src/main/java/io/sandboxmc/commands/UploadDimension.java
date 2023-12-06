package io.sandboxmc.commands;

import java.io.File;
import java.io.IOException;
import java.net.http.HttpHeaders;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.gson.stream.JsonReader;
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

public class UploadDimension {
  public static LiteralArgumentBuilder<ServerCommandSource> register() {
    return CommandManager.literal("upload")
      .then(
        CommandManager.argument("creator", StringArgumentType.word())
        // .suggests(new WebAutoComplete("creators", true))
        .then(
          CommandManager.argument("dimension", StringArgumentType.word())
          // .suggests(new WebAutoComplete("dimensions", "creators", "creator", true))
          .then(
            CommandManager.argument("version", StringArgumentType.word())
            .executes(context -> performUploadCmd(context))
          )
          .executes(context -> performUploadCmd(context))
        )
        .executes(context -> {
          sendFeedback(context.getSource(), Text.literal("No dimension given."));
          return 0;
        })
      )
      .executes(context -> {
        sendFeedback(context.getSource(), Text.literal("No creator or dimension given."));
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

    String creatorName = StringArgumentType.getString(context, "creator");
    // String identifier = StringArgumentType.getString(context, "dimension");
    Session session = ((MinecraftServerAccessor)context.getSource().getServer()).getSession();

    Web web = new Web(context.getSource(), "/dimensions/" + creatorName + "/upload", true);
    try {
      File file = new File(defaultFilePath(session, creatorName, "icon"));
      web.setPostBody("dimension", file);
    } catch (IOException e) {
      sendFeedback(context.getSource(), Text.literal("File not found dummy."));
      return 0;
    }

    try {
      JsonReader jsonReader = web.getJson();
      HttpHeaders headers = web.getResponseHeaders();
      sendFeedback(context.getSource(), Text.literal("Successfully pinged UPLOAD!\nstatus: " + headers.firstValue("status") + "\ncontent type: " + headers.firstValue("content-type")));
    } catch (IOException | InterruptedException e) {
      return 0;
    } finally {
      web.closeReaders();
    }

    return 1;
  }

  private static String defaultFilePath(Session session, String creatorName, String identifier) {
    Path storageFolder = DimensionManager.getStorageFolder(session);
    String creatorFolderName = Paths.get(storageFolder.toString(), creatorName).toString();
    File creatorDirFile = new File(creatorFolderName);
    if (!creatorDirFile.exists()) {
      creatorDirFile.mkdir();
    }

    return Paths.get(storageFolder.toString(), creatorName, identifier + ".png").toString();
  }

  // TODO: Pull this into a more globally available helper...
  private static void sendFeedback(ServerCommandSource source, Text feedbackText) {
    source.sendFeedback(() -> {
      return feedbackText;
    }, false);
  }
}
