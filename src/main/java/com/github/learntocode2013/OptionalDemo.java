package com.github.learntocode2013;

import java.util.Optional;

public class OptionalDemo {

  public static void main(String[] args) {
    String val = Optional.ofNullable(getName())
        .map(n -> {
          System.out.println("Inside map...");
          return n + " Sen";
        })
        .orElse(sensibleDefault());
//        .orElseGet(OptionalDemo::sensibleDefault);
    System.out.printf("Final val: %s %n", val);
  }

  private static String getName() {
    return "Dibakar";
  }

  private static String sensibleDefault() {
    System.out.println("Inside default...");
    return "Software engineer";
  }

}
