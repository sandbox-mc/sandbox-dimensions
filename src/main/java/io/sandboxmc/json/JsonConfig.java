package io.sandboxmc.json;

import java.io.File;
import java.nio.file.Path;
import java.util.HashMap;

public class JsonConfig {
  private HashMap<String, JsonConfigItem> data = new HashMap<>();
  private File file = null;
  private Path filePath = null;

  public JsonConfig() {}

  public JsonConfig(File theFile) {
    filePath = theFile.toPath();
    file = theFile;
  }

  public JsonConfig(Path thePath) {
    filePath = thePath;
    file = new File(filePath.toString());
  }

  public File getFile() {
    return file;
  }
}
