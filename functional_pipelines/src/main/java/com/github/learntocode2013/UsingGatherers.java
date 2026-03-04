package com.github.learntocode2013;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Gatherer;
import lombok.NonNull;

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
}
