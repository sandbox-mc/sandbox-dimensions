package io.sandboxmc.commands;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.gson.stream.JsonReader;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.sandboxmc.Web;
import io.sandboxmc.commands.autoComplete.WebAutoComplete;
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
        .suggests(new WebAutoComplete("creators", true))
        .then(
          CommandManager.argument("dimension", StringArgumentType.word())
          .suggests(new WebAutoComplete("dimensions", "creators", "creator", true))
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
    String creatorName = StringArgumentType.getString(context, "creator");
    // String identifier = StringArgumentType.getString(context, "dimension");
    Session session = ((MinecraftServerAccessor)context.getSource().getServer()).getSession();

    Web web = new Web(context.getSource(), "/dimensions/" + creatorName + "/upload", true);
    try {
      File file = new File(defaultFilePath(session, creatorName, "uploadworld"));
      web.setPostBody(file);
    } catch (FileNotFoundException e) {
      sendFeedback(context.getSource(), Text.literal("File not found dummy."));
      return 0;
    }

    try {
      JsonReader jsonReader = web.getJson();
      sendFeedback(context.getSource(), Text.literal("got through the process"));
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

    return Paths.get(storageFolder.toString(), creatorName, identifier + ".zip").toString();
  }

  // TODO: Pull this into a more globally available helper...
  private static void sendFeedback(ServerCommandSource source, Text feedbackText) {
    source.sendFeedback(() -> {
      return feedbackText;
    }, false);
  }
}
