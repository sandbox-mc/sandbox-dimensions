package io.sandboxmc.commands.datapack;

import java.util.Set;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import io.sandboxmc.datapacks.DatapackManager;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class ListDatapacks {
  public static LiteralArgumentBuilder<ServerCommandSource> register() {
    return CommandManager.literal("list").executes(context -> listDatapacks(context));
  }

  private static int listDatapacks(CommandContext<ServerCommandSource> context) {
    ServerCommandSource source = context.getSource();
    Set<String> enabledEditableDatapacks = DatapackManager.getDatapackNames();

    source.sendFeedback(() -> {
      return Text.literal("Availible Datapacks: ");
    }, false);

    // Send the list of datapacks
    for (String datapackName : enabledEditableDatapacks) {
      context.getSource().sendFeedback(() -> {
        return Text.literal(datapackName);
      }, false);
    }

    return 1;
  }
}
