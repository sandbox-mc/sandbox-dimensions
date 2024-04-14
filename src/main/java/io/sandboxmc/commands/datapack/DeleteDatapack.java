package io.sandboxmc.commands.datapack;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import io.sandboxmc.commands.autoComplete.StringListAutoComplete;
import io.sandboxmc.datapacks.DatapackManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class DeleteDatapack {
  public static LiteralArgumentBuilder<ServerCommandSource> register() {
    return CommandManager.literal("delete").then(
      CommandManager.argument("datapackName", StringArgumentType.word())
      .suggests(new StringListAutoComplete(getDatapackNames()))
      .executes(context -> deleteDatapack(context))
    ).executes(context -> {
      // no arguments given, do nothing
      return 0;
    });
  }

  private static Function<CommandContext<ServerCommandSource>, List<String>> getDatapackNames() {
    return (context) -> {
      List<String> output = new ArrayList<>();
      for (String keyEntry : DatapackManager.getDatapackNames()) {
        output.add(keyEntry);
      }

      return output;
    };
  }

  private static int deleteDatapack(CommandContext<ServerCommandSource> context) {
    ServerCommandSource source = context.getSource();
    String datapackName = StringArgumentType.getString(context, "datapackName");

    if (DatapackManager.deleteDatapack(datapackName)) {
      source.sendFeedback(() -> {
        return Text.literal("Deleted Datapack: " + datapackName);
      }, false);
      return 1;
    }

    source.sendFeedback(() -> {
      return Text.literal("Deleted Datapack Failed for: " + datapackName);
    }, false);
    return 0;
  }
}
