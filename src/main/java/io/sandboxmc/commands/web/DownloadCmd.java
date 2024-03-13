package io.sandboxmc.commands.web;

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
import io.sandboxmc.commands.autoComplete.web.WebAutoComplete;
import io.sandboxmc.datapacks.DatapackManager;
import io.sandboxmc.mixin.MinecraftServerAccessor;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.world.level.storage.LevelStorage.Session;

public class DownloadCmd implements Runnable {
  public static LiteralArgumentBuilder<ServerCommandSource> register() {
    return CommandManager.literal("download")
      .then(
        CommandManager.argument("datapack-identifier", StringArgumentType.greedyString())
        .suggests(new WebAutoComplete("datapacks", true))
        .executes(context -> performDownloadCmd(context))
      )
      .executes(context -> {
        printMessage(context.getSource(), "No datapack specified.");
        return 0;
      });
  }

  private static int performDownloadCmd(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
    ServerCommandSource source = context.getSource();
    String fullIdentifier = StringArgumentType.getString(context, "datapack-identifier");

    Runnable downloadThread = new DownloadCmd(source, fullIdentifier);
    new Thread(downloadThread).start();

    return 1;
  }

  private ServerCommandSource source;
  private String fullIdentifier;

  public DownloadCmd(ServerCommandSource theSource, String theFullIdentifier) {
    source = theSource;
    fullIdentifier = theFullIdentifier;
  }

  public void run() {
    String[] urlItemArray = fullIdentifier.split(":");

    // Make sure the URL is formed properly and can be accessed as an InputStream.
    String fileUrl = "/datapacks";
    Identifier datapackId;

    // Full identifiers must be formatted as:
    // (creator:)datapack
    // TODO:TYLER creators are currently not supported by the web app. This also might share a namespace with "group"?
    // TODO:TYLER allow (:version) also? What would this look like? "vX", a timestamp, custom (parameterized) string?
    // If no creator/group is specified then the datapack must exist in the user's personal collection.
    switch (urlItemArray.length) {
      case 0:
        printMessage(source, "TODO: I don't think this is possible... maybe try to test?");
        return;
      case 1:
        // Case 1, this is for private datapacks, there is no namespace here.
        // In the case of default
        datapackId = new Identifier("personal", urlItemArray[0]);
        fileUrl += "/" + urlItemArray[0];
        System.out.println("Stuff: " + urlItemArray[0]);
        break;
      // case 2:
      //   // Case 2, this is for published datapacks, they would be published under a publisher/group
      //   pathParts[2] = urlItemArray[0];
      //   pathParts[3] = urlItemArray[1];
      //   break;
      default:
        printMessage(source, "Wrong number of identifier parts. Must be `(group:)datapack(:version)`.");
        return;
    }

    // TODO:TYLER don't require auth if there's a creator
    Web web = new Web(source, fileUrl + "/download", true);
    Path filePath = defaultFilePath(); // Currently not supporting anything but default.
    try {
      // This is closed by web.closeReaders()
      BufferedInputStream inputStream = web.getInputStream();
      if (web.getStatusCode() != 200) {
        printMessage(source, "No datapack found at\n" + Web.getWebDomain() + fileUrl + "\nDid you misstype it?");
        return;
      }

      File newFile = new File(filePath.toString());
      if (newFile.exists()) {
        // TODO:TYLER user feedback to overwrite file.
        newFile.delete();
        newFile = new File(filePath.toString());
      }

      FileUtils.copyInputStreamToFile(inputStream, newFile);
    } catch (IOException e) {
      printMessage(source, "No datapack found at\n" + Web.getWebDomain() + fileUrl + "\nDid you misstype it?");
      return; // end early if error
    } finally {
      web.closeReaders();
    }

    DatapackManager.addDownloadedDatapack(datapackId, filePath);
    String downloadCmd = "/sbmc install " + datapackId.toString();

    MutableText feedbackText = Text.literal("Datapack downloaded!\n\n");
    MutableText creationText = Text.literal("[CLICK HERE TO INSTALL IT]");
    ClickEvent unpackEvent = new ClickEvent(ClickEvent.Action.RUN_COMMAND, downloadCmd);
    creationText.setStyle(Style.EMPTY.withClickEvent(unpackEvent));
    creationText.formatted(Formatting.UNDERLINE).formatted(Formatting.BLUE);
    feedbackText.append(creationText);
    printMessage(source, feedbackText);
  }

  private Path defaultFilePath() {
    Session session = ((MinecraftServerAccessor)source.getServer()).getSession();
    Path storageFolder = DatapackManager.getStorageFolder(session);
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
