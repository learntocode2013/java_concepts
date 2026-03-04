package com.github.learntocode2013;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Gatherer;
import java.util.stream.Gatherer.Downstream;
import lombok.NonNull;

public class UsingGatherers {
  private static final String LS = System.lineSeparator();

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

  public record ValueWithIndex<E>(E value, int index) {
    @NonNull
    @Override
    public String toString() {
      return "%d: %s".formatted(index, value);
    }
  }

  public static <T, R> Gatherer<? super T, AtomicInteger, ValueWithIndex<R>>
  mapWithIndex(Function<T, R> mapper) {
    return Gatherer.ofSequential(
        AtomicInteger::new,
        (index, element, downstream) ->
            downstream.push(new ValueWithIndex<R>(
                mapper.apply(element),
                index.getAndIncrement()
            ))
        );
  }

  public static <T> Gatherer<T, ?, T> takeAnyOneMatching(
      Predicate<T> predicate) {
    return Gatherer.of((_, element, downstream)
        -> pushIfMatch(predicate, element, downstream));
  }

  private static <T> boolean pushIfMatch(
      Predicate<T> predicate,
      T element,
      Downstream<? super T> downstream) {
    if (predicate.test(element)) {
      downstream.push(element);
      System.out.printf("Thread: %s | Element pushed: %s %s",
          Thread.currentThread().getName(),
          element,
          LS);
      return false;
    }
    return true;
  }
}
