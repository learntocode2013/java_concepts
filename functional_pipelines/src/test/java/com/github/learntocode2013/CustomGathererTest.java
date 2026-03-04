package com.github.learntocode2013;

import java.util.Locale;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CustomGathererTest {
  private static final String LS = System.lineSeparator();

  @Test
  void demo_sequential_stateless_gatherer() {
    System.out.println("-----------sequential_stateless_gatherer---------");
    var sum = Stream.of(10, 20, 30, 40, 50)
        .parallel()
        .peek(System.out::println)
        .reduce(0, Integer::sum);
    System.out.printf("Sum: %d %s",  sum, LS);
    System.out.println("------------------------------");
    sum = Stream.of(10, 20, 30, 40, 50)
        .parallel()
        .gather(UsingGatherers.peekInOrder(System.out::println))
        .reduce(0, Integer::sum);
    System.out.printf("[Using peekInOrder] Sum: %d %s",  sum, LS);
  }

  @Test
  void demo_sequential_stateful_gatherer() {
    System.out.println("-----------sequential_stateful_gatherer---------");
    Stream.of("Nick", "Tomas", "Venket", "Dibakar", "Frank")
        .parallel()
        .filter(n -> n.length() > 4)
        .gather(UsingGatherers.<String,String>mapWithIndex(
            e -> e.toUpperCase(Locale.ENGLISH)))
        .forEachOrdered(System.out::println);
    System.out.println("--------------------------");
  }

  @Test
  void demo_parallel_stateless_gatherer() {
    System.out.println("-----------parallel_stateless_gatherer---------");
    Stream.of(10, 11, 15, 17, 21, 25, 27, 55, 60)
        .parallel()
        .gather(UsingGatherers.<Integer>takeAnyOneMatching(
            e -> e > 25))
        .map(e -> e * 10)
        .peek(System.out::println)
        .findFirst()
        .ifPresent(val -> Assertions.assertEquals(270, val));
    System.out.println();
  }
}
