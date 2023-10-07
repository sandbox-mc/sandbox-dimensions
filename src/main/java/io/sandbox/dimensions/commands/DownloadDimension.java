package io.sandbox.dimensions.commands;

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

import io.sandbox.dimensions.mixin.MinecraftServerAccessor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.level.storage.LevelStorage.Session;

public class DownloadDimension {
  public static LiteralArgumentBuilder<ServerCommandSource> register() {
    return CommandManager.literal("download")
      .then(
        CommandManager.argument("url", StringArgumentType.word())
        .executes(ctx -> performDownloadCmd(
          StringArgumentType.getString(ctx, "url"),
          null,
          ctx.getSource()
        ))
      )
      .executes(context -> {
        System.out.println("Fallback????");
        return 1;
      });
  }

  private static int performDownloadCmd(String theUrl, @Nullable String customFileName, ServerCommandSource source) throws CommandSyntaxException {
    // Make sure the URL is formed properly and can be accessed as an InputStream.
    URL url;
    InputStream inputStream;
    try {
      url = new URL(theUrl);
      inputStream = url.openStream();
    } catch (MalformedURLException e) {
      return 0;
    } catch (IOException e) {
      return 0;
    }

    // Pull the URL into a channel
    ReadableByteChannel readableByteChannel = Channels.newChannel(inputStream);

    FileOutputStream fileOutputStream;
    try {
      Path filePath = getFileDestination(source, "butts.zip");
      fileOutputStream = new FileOutputStream(filePath.toString());
    } catch (FileNotFoundException e) {
      return 0;
    }
  
    FileChannel fileChannel = fileOutputStream.getChannel();

    try {
      fileChannel.transferFrom(readableByteChannel, 0, Long.MAX_VALUE);
      fileOutputStream.close();
    } catch (IOException e) {
      return 0;
    }

    return 1;
  }

  private static Path getFileDestination(ServerCommandSource source, String fileName) {
    MinecraftServer server = source.getServer();
    Session session = ((MinecraftServerAccessor)server).getSession();
    Path dimensionSavePath = Paths.get(
      session.getDirectory(WorldSavePath.DATAPACKS).getParent().toString(),
      "dimensions",
      "downloads",
      fileName
    );

    return dimensionSavePath;
  }
}
