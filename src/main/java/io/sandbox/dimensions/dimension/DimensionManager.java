package io.sandbox.dimensions.dimension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.sandbox.dimensions.Main;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;

public class DimensionManager {
  private static List<String> initializedDimensions = new ArrayList<>();
  private static List<Identifier> sandboxDimensionWorldFiles = new ArrayList<>();
  private static Map<Identifier, DimensionSave> dimensionSaves = new HashMap<>();

  public static void init() {
    ResourceManagerHelper.get(ResourceType.SERVER_DATA)
    .registerReloadListener(new SimpleSynchronousResourceReloadListener() {
      @Override
      public Identifier getFabricId() {
        return Main.id("world_save_loader");
      }

      @Override
      public void reload(ResourceManager manager) {
        Map<Identifier, Resource> dimensions = manager.findResources("dimension", path -> true);
        for (Identifier dimensionName : dimensions.keySet()) {
          System.out.println("Dimension: " + dimensionName);
          initializedDimensions.add(dimensionName.toString());
        }
        // test_realm:world_saves/my_world.zip
        // Load all template pools for reference later
        Map<Identifier, Resource> worldSaves = manager.findResources("world_saves", path -> true);
        for (Identifier resourceName : worldSaves.keySet()) {
          String dataPackName = resourceName.getNamespace();
          System.out.println("TEst: " + worldSaves.get(resourceName).getResourcePackName() + " : " + resourceName.getPath());

          // Resource resource = worldSaves.get(resourceName);
          // Pathing should match for Namespace and fileName
          String dimensionIdentifier = resourceName.toString()
            .replaceAll("world_saves", "dimension")
            .replaceAll(".zip", ".json");
          if (initializedDimensions.contains(dimensionIdentifier)) {
            System.out.println("Loaded: " + dataPackName + " : " + resourceName);
            if (!sandboxDimensionWorldFiles.contains(resourceName)) {
              sandboxDimensionWorldFiles.add(resourceName);
            }
          }
        }
      }
    });
  }

  public static void processSandboxDimensionFiles(ServerWorld server) {
    var storage = server.getPersistentStateManager();
    for (Identifier dimensionId : sandboxDimensionWorldFiles) {
      DimensionSave dimensionSave = new DimensionSave();
      dimensionSaves.put(dimensionId, dimensionSave);
    }
  }
}
