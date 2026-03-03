package com.github.learntocode2013.lights;

public class TrafficLightFactory {
  public static TrafficLight createVerticalTrafficLight() {
    return new VerticalTrafficLight();
  }

  public static TrafficLight createHorizontalTrafficLight() {
    return new HorizontalTrafficLight();
  }
}
