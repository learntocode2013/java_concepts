package com.github.learntocode2013.power;

public sealed interface PowerSource permits
    ElectricGridSource,
    GreenPower,
    ExperimentalPower {
  void drawEnergy();
}
