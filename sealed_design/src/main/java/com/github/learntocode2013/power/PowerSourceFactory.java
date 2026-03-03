package com.github.learntocode2013.power;

public class PowerSourceFactory {
  public static PowerSource createNuclearPowerSource() {
    return new MiniatureNuclearPower();
  }
}
