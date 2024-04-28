package io.sandboxmc.dimension.configs;

import io.sandboxmc.dimension.GameRuleOverrides;
import net.minecraft.world.GameRules;

public class GameRuleConfig {
  public Boolean doMobSpawning;

  public GameRuleOverrides applyToOverrides(GameRuleOverrides overrides) {
    if (this.doMobSpawning != null) {
      overrides.set(GameRules.DO_MOB_SPAWNING, this.doMobSpawning);
    }

    return overrides;
  }
}
