package com.github.learntocode2013.lights;

public sealed abstract class TrafficLight permits
    VerticalTrafficLight,
    HorizontalTrafficLight,
    RailRoadLight {
  public void turnRed() {}
  public void turnGreen() {}
  public void turnYello() {}
}

final class VerticalTrafficLight extends TrafficLight {}
final class HorizontalTrafficLight extends TrafficLight {}
