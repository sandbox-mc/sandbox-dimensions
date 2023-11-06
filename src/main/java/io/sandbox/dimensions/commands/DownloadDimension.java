package io.sandbox.dimensions.commands;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
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
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;

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
          System.out.println("Download was called with creator but no dimension identifier.");
          return 1;
        })
      )
      .executes(context -> {
        System.out.println("Download was called with no arguments.");
        return 1;
      });
  }

  private static int performDownloadCmd(String creatorName, String identifier, @Nullable String customFileName, ServerCommandSource source) throws CommandSyntaxException {
    // Make sure the URL is formed properly and can be accessed as an InputStream.
    URL url;
    InputStream inputStream;
    try {
      url = new URL("https://www.sandboxmc.io/dimensions/" + creatorName + "/" + identifier + "/download");
      inputStream = url.openStream();
    } catch (MalformedURLException e) {
      System.out.println("MalformedURL");
      return 0;
    } catch (IOException e) {
      System.out.println("IOException from input stream");
      return 0;
    }

    // Pull the URL into a channel
    ReadableByteChannel readableByteChannel = Channels.newChannel(inputStream);

    FileOutputStream fileOutputStream;

    String filePath;
    if (customFileName == null) {
      filePath = defaultFilePath(source, creatorName, identifier);
    } else {
      filePath = customFilePath(source, customFileName);
    }

    try {
      fileOutputStream = new FileOutputStream(filePath);
    } catch (FileNotFoundException e) {
      // This can't actually happen, customFilePath and defaultFilePath both ensure everything.
      System.out.println("DID WE GET HERE...?");
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

    return 1;
  }

  private static String defaultFilePath(ServerCommandSource source, String creatorName, String identifier) {
    Path storageFolder = DimensionManager.getStorageFolder(source);
    String creatorFolderName = Paths.get(storageFolder.toString(), creatorName).toString();
    File creatorDirFile = new File(creatorFolderName);
    if (!creatorDirFile.exists()) {
      creatorDirFile.mkdir();
    }

    return Paths.get(storageFolder.toString(), creatorName, identifier + ".zip").toString();
  }

  private static String customFilePath(ServerCommandSource source, String customFileName) {
    Path storageFolder = DimensionManager.getStorageFolder(source);

    if (!customFileName.endsWith(".zip")) {
      customFileName += ".zip";
    }

    String filePath = Paths.get(storageFolder.toString(), customFileName).toString();

    return filePath;
  }
}
