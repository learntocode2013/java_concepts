package com.github.learntocode2013;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class DiceScores {
  private static final Logger logger = Logger.getLogger(DiceScores.class.getName());
  public enum Pip {
    ONE,
    TWO,
    THREE,
    FOUR,
    FIVE,
    SIX
  }
  public static int scoreForDicePip(Pip pip) {
    return switch (pip) {
      case Pip.ONE -> {
        logger.log(Level.WARNING, "High score observed - %d".formatted(100));
        yield 100;
      }
      case Pip.TWO -> 2;
      case Pip.THREE, Pip.FOUR -> 3;
      case Pip.FIVE -> 5;
      case Pip.SIX -> 50;
    };
  }

  public static void main(String[] args) {
    Stream.of(
        Pip.ONE, Pip.TWO, Pip.THREE, Pip.FOUR, Pip.FIVE, Pip.SIX)
        .map(DiceScores::scoreForDicePip)
        .forEach(System.out::println);
  }
}
