package io.sandboxmc.dimension;

import net.minecraft.world.Difficulty;
import net.minecraft.world.GameRules;
import net.minecraft.world.SaveProperties;
import net.minecraft.world.level.UnmodifiableLevelProperties;

public class SandboxWorldProperties extends UnmodifiableLevelProperties {
  protected SandboxWorldConfig config;
  private final GameRules rules;

  public SandboxWorldProperties(SaveProperties saveProperties, SandboxWorldConfig config) {
    super(saveProperties, saveProperties.getMainWorldProperties());
    this.config = config;
    this.rules = new GameRules();
    config.getGameRules().applyTo(rules, null);
  }
  
  @Override
  public GameRules getGameRules() {
    return this.rules;
  }

  @Override
  public void setTimeOfDay(long timeOfDay) {
    this.config.setTimeOfDay(timeOfDay);
  }

  @Override
  public long getTimeOfDay() {
    return this.config.getTimeOfDay();
  }

  @Override
  public void setClearWeatherTime(int time) {
    this.config.setSunny(time);
  }

  @Override
  public int getClearWeatherTime() {
    return this.config.getSunnyTime();
  }

  @Override
  public void setRaining(boolean raining) {
    this.config.setRaining(raining);
  }

  @Override
  public boolean isRaining() {
    return this.config.getRaining();
  }

  @Override
  public void setRainTime(int time) {
    this.config.setRaining(time);
  }

  @Override
  public int getRainTime() {
    return this.config.getRainTime();
  }

  @Override
  public void setThundering(boolean thundering) {
    this.config.setThundering(thundering);
  }

  @Override
  public boolean isThundering() {
    return this.config.getThundering();
  }

  @Override
  public void setThunderTime(int time) {
    this.config.setThundering(time);
  }

  @Override
  public int getThunderTime() {
    return this.config.getThunderTime();
  }

  @Override
  public Difficulty getDifficulty() {
    Difficulty difficulty = this.config.getDifficulty();
    if (difficulty != null) {
      return difficulty;
    }

    return super.getDifficulty();
  }
}
