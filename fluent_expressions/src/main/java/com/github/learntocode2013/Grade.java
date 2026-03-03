package com.github.learntocode2013;

import java.util.stream.Stream;

public class Grade {
  public static String gradeFor(int score) {
    int expressionValue = Math.min(score/10,10);
    final String gradeForScore = switch (expressionValue) {
      case 10, 9 -> "A";
      case 8 -> "B";
      case 7 -> "C";
      case 6 -> "D";
      case 5 -> "E";
      default -> "F";
    };
    return "Grade for score: %d is %s".formatted(score, gradeForScore);
  }

  public static void main(String[] args) {
    Stream.of(
        59, 69, 79, 89, 99, 100, 49, 40)
        .map(Grade::gradeFor)
        .forEach(System.out::println);
  }
}
