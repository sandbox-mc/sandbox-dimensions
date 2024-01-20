package io.sandboxmc.datapacks.types;

import java.nio.file.Path;

import net.minecraft.util.Identifier;

public class DownloadedPack {
  public Identifier packIdentifier;
  public Path installFile;

  public DownloadedPack(Identifier packIdentifier, Path installFile) {
    this.packIdentifier = packIdentifier;
    this.installFile = installFile;
  }
}
