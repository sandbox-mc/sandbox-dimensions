package io.sandboxmc.commands;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;

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
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.level.storage.LevelStorage.Session;

public class DownloadDimension implements Runnable {
  public static LiteralArgumentBuilder<ServerCommandSource> register() {
    return CommandManager.literal("download")
      .then(
        CommandManager.argument("dimension-identifier", StringArgumentType.greedyString())
        .suggests(new WebAutoComplete("dimensions", true))
        .executes(context -> performDownloadCmd(context))
      )
      .executes(context -> {
        printMessage(context.getSource(), "No dimension specified.");
        return 0;
      });
  }

  private static int performDownloadCmd(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerCommandSource source = context.getSource();
    String fullIdentifier = StringArgumentType.getString(context, "dimension-identifier");

    Runnable downloadThread = new DownloadDimension(source, fullIdentifier);
    new Thread(downloadThread).start();

    return 1;
  }

  private ServerCommandSource source;
  private String fullIdentifier;

  public DownloadDimension(ServerCommandSource theSource, String theFullIdentifier) {
    source = theSource;
    fullIdentifier = theFullIdentifier;
  }

  public void run() {
    String[] creatorDimensionAry = fullIdentifier.split(":");

    // Make sure the URL is formed properly and can be accessed as an InputStream.
    String fileUrl = "/dimensions";

    // Full identifiers must be formatted as:
    // (creator:)dimension
    // TODO: creators are currently not supported by the web app. This also might share a namespace with "group"?
    // TODO: allow (:version) also? What would this look like? "vX", a timestamp, custom (parameterized) string?
    // If no creator/group is specified then the dimension must exist in the user's personal collection.
    switch (creatorDimensionAry.length) {
      case 0:
        printMessage(source, "TODO: I don't think this is possible... maybe try to test?");
        return;
      case 1:
        fileUrl += "/" + creatorDimensionAry[0];
        break;
      // case 2:
      //   pathParts[2] = creatorDimensionAry[0];
      //   pathParts[3] = creatorDimensionAry[1];
      //   break;
      default:
        printMessage(source, "Wrong number of identifier parts. Must be `(group:)dimension(:version)`.");
        return;
    }

    // TODO: don't require auth if there's a creator
    Web web = new Web(source, fileUrl + "/download", true);
    Path filePath = defaultFilePath(); // Currently not supporting anything but default.
    try {
      // This is closed by web.closeReaders()
      BufferedInputStream inputStream = web.getInputStream();
      if (web.getStatusCode() != 200) {
        printMessage(source, "No dimension found at\n" + Web.WEB_DOMAIN + fileUrl + "\nDid you misstype it?");
        return;
      }

      File newFile = new File(filePath.toString());
      if (newFile.exists()) {
        // TODO: user feedback to overwrite file.
        newFile.delete();
        // TODO: do I have to re-initialize it after deletion?
        newFile = new File(filePath.toString());
      }

      FileUtils.copyInputStreamToFile(inputStream, newFile);
    } catch (IOException e) {
      printMessage(source, "No dimension found at\n" + Web.WEB_DOMAIN + fileUrl + "\nDid you misstype it?");
    } finally {
      web.closeReaders();
    }

    MutableText feedbackText = Text.literal("Dimension downloaded!\n\n");
    MutableText creationText = Text.literal("[CLICK HERE TO INSTALL IT]");
    // TODO: We need an "install" command which installs from a downloaded file.
    // Start with the commented code block below...
    ClickEvent unpackEvent = new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/dimension install xyz");
    creationText.setStyle(Style.EMPTY.withClickEvent(unpackEvent));
    creationText.formatted(Formatting.UNDERLINE).formatted(Formatting.BLUE);
    feedbackText.append(creationText);
    printMessage(source, feedbackText);

    // Path datapackPath = session.getDirectory(WorldSavePath.DATAPACKS);
    // datapackPath = Paths.get(datapackPath.toString(), identifier);
    // try {
    //   DatapackManager.installDatapackFromZip(filePath, datapackPath);
    // } catch (IOException e) {
    //   // TODO Auto-generated catch block
    //   e.printStackTrace();
    // }
  }

  private Path defaultFilePath() {
    Session session = ((MinecraftServerAccessor)source.getServer()).getSession();
    Path storageFolder = DimensionManager.getStorageFolder(session);
    String[] pathParts = fullIdentifier.split(":");

    Path currentPath = storageFolder;
    // all but last item, that would be the actual file name
    for (int i = 0; i < pathParts.length - 1; i++) {
      currentPath = Paths.get(currentPath.toString(), pathParts[i]);
      File dirFile = new File(currentPath.toString());
      if (!dirFile.exists()) {
        dirFile.mkdir();
      }
    }

    return Paths.get(currentPath.toString(), pathParts[pathParts.length - 1] + ".zip");
  }

  private static void printMessage(ServerCommandSource source, MutableText message) {
    source.sendFeedback(() -> {
      return message;
    }, false);
  }

  private static void printMessage(ServerCommandSource source, String message) {
    printMessage(source, Text.literal(message));
  }
}
