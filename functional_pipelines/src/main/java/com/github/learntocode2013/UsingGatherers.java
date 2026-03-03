package com.github.learntocode2013;

import java.util.function.Consumer;
import java.util.stream.Gatherer;

public class UsingGatherers {
  public static <T> boolean consumeAndPush(
      T element,
      Consumer<T> consumer,
      Gatherer.Downstream<? super T> downstream
  ) {
      consumer.accept(element);
      return downstream.push(element);
  }

  public static <T> Gatherer<T, ?, T> peekInOrder(
      Consumer<T> consumer) {
   return Gatherer.ofSequential((_, element, downstream) ->
     consumeAndPush(element, consumer, downstream));
  }
}
