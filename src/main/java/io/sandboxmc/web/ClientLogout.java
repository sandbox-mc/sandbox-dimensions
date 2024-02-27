package io.sandboxmc.web;

import java.io.IOException;

import com.mojang.brigadier.context.CommandContext;

import io.sandboxmc.Web;
import net.minecraft.server.command.ServerCommandSource;

public class ClientLogout extends Common implements Runnable {
  
  public ClientLogout(CommandContext<ServerCommandSource> theContext) {
    super(theContext);
  }

  public void run() {
    ServerCommandSource source = context.getSource();

    BearerToken bearerToken = Web.getBearerToken(source.getPlayer());
    if (bearerToken == null) {
      printMessage("Not currently logged in.");
      return;
    }

    Web web = new Web(source, "/mc/client/auth/logout", bearerToken.getToken());
    web.setDeleteBody();

    try {
      web.executeRequest();
    } catch (IOException e) {
      // Can safely ignore.
    } finally {
      web.closeReaders();
    }

    Web.removeBearerToken(source.getPlayer());

    printMessage("Logged out of SandboxMC.");
  }
}
