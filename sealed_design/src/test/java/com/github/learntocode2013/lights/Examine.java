package com.github.learntocode2013.lights;

import com.github.learntocode2013.power.PowerSource;
import com.github.learntocode2013.power.PowerSourceFactory;
import java.util.Optional;

public class Examine {
  public static final String LS = System.lineSeparator();

  public static void printInfo(Class<? extends TrafficLight> klass) {
    System.out.printf("Class: %s %s", klass.getCanonicalName(), LS);
    System.out.printf("Is sealed ? %s %s", klass.isSealed(), LS);

    Class<?>[] permittedSubclasses = klass.getPermittedSubclasses();
    Optional.ofNullable(permittedSubclasses)
        .ifPresent(subclasses -> {
          System.out.printf("--- Permitted subclasses are ---%s", LS);
          for(Class<?> permittedSubclass : subclasses) {
            System.out.printf("Permitted subclass: %s %s",
                permittedSubclass.getCanonicalName(), LS);
          }
        });
  }

  private static void demoPowerSource(PowerSource powerSource) {
    powerSource.drawEnergy();
  }

  public static void main(String[] args) {
    printInfo(TrafficLightFactory.createVerticalTrafficLight().getClass());
    System.out.println("-----------------------");
    printInfo(TrafficLightFactory.createHorizontalTrafficLight().getClass());
    System.out.println("-----------------------");
    printInfo(TrafficLight.class);
    System.out.println("-----------------------");
    demoPowerSource(PowerSourceFactory.createNuclearPowerSource());
  }
}
