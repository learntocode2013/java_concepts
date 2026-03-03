package com.github.learntocode2013;

import java.util.stream.Stream;
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
}
