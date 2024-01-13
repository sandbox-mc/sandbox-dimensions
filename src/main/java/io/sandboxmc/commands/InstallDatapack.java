package io.sandboxmc.commands;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import io.sandboxmc.commands.autoComplete.StringListAutoComplete;
import io.sandboxmc.datapacks.DatapackManager;
import io.sandboxmc.mixin.MinecraftServerAccessor;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;
import net.minecraft.world.level.storage.LevelStorage.Session;

public class InstallDatapack {
  public static LiteralArgumentBuilder<ServerCommandSource> register() {
    return CommandManager.literal("install").then(
      CommandManager
      .argument("datapack", StringArgumentType.word())
      .suggests(new StringListAutoComplete(getDownloadedDatapackAutocomplete()))
      .then(
        CommandManager
        .argument("overwrite", BoolArgumentType.bool())
        .executes(context -> installDatapack(
          context,
          StringArgumentType.getString(context, "datapack"),
          BoolArgumentType.getBool(context, "overwrite"))
        )
      )
      .executes(context -> installDatapack(
        context,
        StringArgumentType.getString(context, "datapack"),
        null
      ))
    ).executes(context -> {
      // TODO: add fallback messaging
      System.out.println("Fallback????");
      return 1;
    });
  }

  private static Function<CommandContext<ServerCommandSource>, List<String>> getDownloadedDatapackAutocomplete() {
    return (context) -> {
      List<String> output = new ArrayList<>();
      for (String keyEntry : DatapackManager.getDownloadedDatapacks()) {
        output.add(keyEntry);
      }

      return output;
    };
  }

  private static int installDatapack(CommandContext<ServerCommandSource> context, String datapackName, Boolean shouldOverwrite) {
    ServerCommandSource source = context.getSource();
    System.out.println("Testing...");
    System.out.println("OUT: " + datapackName + " : " + shouldOverwrite);
    // System.out.println("OUT: " + datapackName);
    source.sendFeedback(() -> {
      return Text.literal("Installed Dimension: " + datapackName);
    }, false);
    return 1;
  }
}
