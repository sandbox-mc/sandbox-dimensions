package io.sandbox.dimensions.commands;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.jetbrains.annotations.Nullable;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import io.sandbox.dimensions.commands.zip.UnzipUtility;
import io.sandbox.dimensions.mixin.MinecraftServerAccessor;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.level.storage.LevelStorage.Session;

public class RestoreDimension {
  public static LiteralArgumentBuilder<ServerCommandSource> register() {
    return CommandManager.literal("restore")
      .then(
        CommandManager.argument("file", StringArgumentType.word())
        .executes(ctx -> execute(
          StringArgumentType.getString(ctx, "file"),
          null,
          ctx.getSource()
        ))
      )
      .executes(context -> {
        System.out.println("Fallback????");
        return 1;
      });
    }
    
    private static int execute(String file, @Nullable String comment, ServerCommandSource source) throws CommandSyntaxException {
    MinecraftServer server = source.getServer();
    // ServerLifecycleEvents
    // server.getSaveProperties().;
    // The session is what owns the lock on the folders
    // need to kill that lock
    Session session = ((MinecraftServerAccessor)server).getSession();
    // session.getLevelStorage()
    // Path the the save dir...
    Path dimensionSavePath = Paths.get(
      session.getDirectory(WorldSavePath.DATAPACKS).getParent().toString(),
      "dimensions",
      "test_realm",
      "test_realm"
    );
    Path datapackLoadFilePath = Paths.get(
      session.getDirectory(WorldSavePath.DATAPACKS).toString(),
      "test_realm",
      "data",
      "test_realm",
      "world_save",
      "my_world.zip"
    );
        
    System.out.println("DataPack Dir: " + datapackLoadFilePath); // full path
    // Path tmp;

    // try {
    //   // Create tmp file for writing the Zip to
    //   tmp = Files.createTempDirectory(dimensionSavePath, "my_world.zip");
    // } catch (IOException e) {
    //   System.out.println("IO Exception from tmp creation");
    //   e.printStackTrace();
    //   return 0; // failed
    // }

    //By making a separate thread we can start unpacking an old backup instantly
    //Let the server shut down gracefully, and wait for the old world backup to complete
    // FutureTask<Void> waitForShutdown = new FutureTask<>(() -> {
    //   server.getThread().join(); //wait for server thread to die and save all its state
    //   return null;
    // });

    // //run the thread.
    // new Thread(waitForShutdown, "Server shutdown wait thread").start();

    try {
      
      // Wait for server to stop
      // waitForShutdown.get();
      
      // // Something happened...
      // if (Files.notExists(tmp)) {
      //   System.out.println("File not found");
      //   return 1;
      // }
      // server.submitAndJoin(() -> {
      //   server.saveAll(true, true, true);
      // });

      
      // server.getSer
      // for (ServerWorld serverWorld : server.getWorlds()) {
      //   if (serverWorld != null && !serverWorld.savingDisabled) {
      //     System.out.println("World: ");
      //     serverWorld.save(null, true, false);
      //     // serverWorld.setChunkForced();
      //   }
      // }

      // session.close();
      
      // delete current dimension save
      // Maybe make this a backup process or something???
      UnzipUtility.deleteDirectory(dimensionSavePath);

      // Unzip the world files
      // UnzipUtility unzipper = new UnzipUtility();
      System.out.println("Starting unzip");
      UnzipUtility.unzipFile(datapackLoadFilePath, dimensionSavePath);
      System.out.println("Done unzipping");


      // Files.move(tmp, dimensionSavePath);
    } catch (IOException e) {
      System.out.println("IO Exception");
      e.printStackTrace();
    }
    // catch (InterruptedException e) {
    //   System.out.println("InterruptedException");
    //   e.printStackTrace();
    // } catch (ExecutionException e) {
    //   System.out.println("ExecutionException");
    //   e.printStackTrace();
    // }

    return 1;
  }
}
