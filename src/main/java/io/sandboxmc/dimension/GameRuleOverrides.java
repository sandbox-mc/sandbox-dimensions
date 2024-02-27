package io.sandboxmc.dimension;

import org.jetbrains.annotations.Nullable;

import it.unimi.dsi.fastutil.objects.Reference2BooleanMap;
import it.unimi.dsi.fastutil.objects.Reference2BooleanMaps;
import it.unimi.dsi.fastutil.objects.Reference2BooleanOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntMaps;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.GameRules;

public class GameRuleOverrides {
  private final Reference2BooleanMap<GameRules.Key<GameRules.BooleanRule>> booleanRules = new Reference2BooleanOpenHashMap<>();
  private final Reference2IntMap<GameRules.Key<GameRules.IntRule>> intRules = new Reference2IntOpenHashMap<>();

  public void set(GameRules.Key<GameRules.BooleanRule> key, boolean value) {
    this.booleanRules.put(key, value);
  }

  public void set(GameRules.Key<GameRules.IntRule> key, int value) {
    this.intRules.put(key, value);
  }

  public boolean getBoolean(GameRules.Key<GameRules.BooleanRule> key) {
    return this.booleanRules.getBoolean(key);
  }

  public int getInt(GameRules.Key<GameRules.IntRule> key) {
    return this.intRules.getInt(key);
  }

  public boolean contains(GameRules.Key<?> key) {
    return this.booleanRules.containsKey(key) || this.intRules.containsKey(key);
  }

  public void applyTo(GameRules rules, @Nullable MinecraftServer server) {
    Reference2BooleanMaps.fastForEach(this.booleanRules, entry -> {
      GameRules.BooleanRule rule = rules.get(entry.getKey());
      rule.set(entry.getBooleanValue(), server);
    });

    Reference2IntMaps.fastForEach(this.intRules, entry -> {
      GameRules.IntRule rule = rules.get(entry.getKey());
      rule.set(entry.getIntValue(), server);
    });
  }
}
