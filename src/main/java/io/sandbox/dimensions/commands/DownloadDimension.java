package io.sandbox.dimensions.commands;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jetbrains.annotations.Nullable;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.sandbox.dimensions.dimension.DimensionManager;
import io.sandbox.dimensions.mixin.MinecraftServerAccessor;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.level.storage.LevelStorage.Session;

public class DownloadDimension {
  public static LiteralArgumentBuilder<ServerCommandSource> register() {
    return CommandManager.literal("download")
      .then(
        CommandManager.argument("creator", StringArgumentType.word())
        .then(
          CommandManager.argument("dimension", StringArgumentType.word())
          .then(
            CommandManager.argument("customFileName", StringArgumentType.word())
            .executes(context -> performDownloadCmd(
              StringArgumentType.getString(context, "creator"),
              StringArgumentType.getString(context, "dimension"),
              StringArgumentType.getString(context, "customFileName"),
              context.getSource())
            )
          )
          .executes(context -> performDownloadCmd(
            StringArgumentType.getString(context, "creator"),
            StringArgumentType.getString(context, "dimension"),
            null,
            context.getSource())
          )
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

  private static int performDownloadCmd(String creatorName, String identifier, @Nullable String customFileName, ServerCommandSource source) throws CommandSyntaxException {
    // Make sure the URL is formed properly and can be accessed as an InputStream.
    String dimensionShow = "https://www.sandboxmc.io/dimensions/" + creatorName + "/" + identifier;

    URL url;
    InputStream inputStream;
    try {
      url = URI.create(dimensionShow + "/download");
      inputStream = url.openStream();
    } catch (IOException e) {
      sendFeedback(source, Text.literal("No dimension found at\n" + dimensionShow + "\nDid you misstype it?"));
      return 0;
    }

    // Pull the URL into a channel
    ReadableByteChannel readableByteChannel = Channels.newChannel(inputStream);

    FileOutputStream fileOutputStream;

    String filePath;
    Session session = ((MinecraftServerAccessor)source.getServer()).getSession();
    if (customFileName == null) {
      filePath = defaultFilePath(session, creatorName, identifier);
    } else {
      filePath = customFilePath(session, customFileName);
    }

    try {
      fileOutputStream = new FileOutputStream(filePath);
    } catch (FileNotFoundException e) {
      // This can't actually happen, customFilePath and defaultFilePath both ensure everything.
      // Existing files are replaced.
      return 0;
    }
  
    FileChannel fileChannel = fileOutputStream.getChannel();

    try {
      fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
      fileOutputStream.close();
    } catch (IOException e) {
      System.out.println("io exception reading file from channel");
      return 0;
    }

    MutableText feedbackText = Text.literal("Dimension downloaded!\n\n");
    MutableText creationText = Text.literal("[CLICK HERE TO CREATE IT]");
    // TODO: Need to run some sort of unpack method here.
    ClickEvent unpackEvent = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/dimension download awef awef");
    creationText.setStyle(Style.EMPTY.withClickEvent(unpackEvent));
    creationText.formatted(Formatting.UNDERLINE).formatted(Formatting.BLUE);
    feedbackText.append(creationText);
    sendFeedback(source, feedbackText);

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

  private static String customFilePath(Session session, String customFileName) {
    Path storageFolder = DimensionManager.getStorageFolder(session);

    if (!customFileName.endsWith(".zip")) {
      customFileName += ".zip";
    }

    String filePath = Paths.get(storageFolder.toString(), customFileName).toString();

    return filePath;
  }

  private static void sendFeedback(ServerCommandSource source, Text feedbackText) {
    source.sendFeedback(() -> {
      return feedbackText;
    }, false);
  }
}
