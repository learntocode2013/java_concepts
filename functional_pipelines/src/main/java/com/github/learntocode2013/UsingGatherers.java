package com.github.learntocode2013;

import java.time.Duration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Gatherer;
import java.util.stream.Gatherer.Downstream;
import java.util.stream.Gatherers;
import java.util.stream.Stream;
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

  public record Person(String name, int age) {
    public int ageGroup() {
      return (age/10) * 10;
    }
  }

  public static List<Person> useDistinctBy(List<Person> people) {
    return people
        .parallelStream()
        .gather(distinctBy(Person::ageGroup))
        .collect(Collectors.toList());
  }

  public static List<Person> useDistinctByParallel(List<Person> people) {
    return people
        .parallelStream()
        .gather(distinctByParallel(Person::ageGroup))
        .collect(Collectors.toList());
  }

  private static <T, C extends Comparable<C>> Gatherer<? super T, ?, T>  distinctBy(
      Function<T, C> criteria) {
    return Gatherer.ofSequential(
        HashSet::new,
        (state, element, downstream) ->
            !state.add(criteria.apply(element)) || downstream.push(element)
        );
  }

  public static <T, C extends Comparable<C>> Gatherer<? super T, ? , T>
  distinctByParallel(Function<T, C> criteria) {
    return Gatherer.of(
        DistinctValues<T>::new,
        (state, element, _) ->
            state.addIfDistinct(element, criteria),
        (state1, state2) ->
            state1.combineDistinct(state2, criteria),
        DistinctValues::pushEachValueDownstream
    );
  }

  static class DistinctValues<T> {
    private final Set<T> distinctElements = new HashSet<>();

    public <C extends Comparable<C>> boolean addIfDistinct(
        T element,
        Function<T, C> criteria) {
      if(distinctElements.stream()
          .noneMatch(existing -> criteria.apply(existing).compareTo(criteria.apply(element)) == 0)) {
        distinctElements.add(element);
      }
      return true;
    }

    public <C extends Comparable<C>> DistinctValues<T> combineDistinct(
        DistinctValues<T> toCombine,
        Function<T, C> criteria
    ) {
      for(T element : toCombine.distinctElements) {
        addIfDistinct(element, criteria);
      }
      return this;
    }

    public void pushEachValueDownstream(
        Gatherer.Downstream<? super T> downstream
    ) {
        for(T element : distinctElements) {
          if (!downstream.push(element)) {
            break;
          }
        }
    }

  }

  public record Movie(String name, int rating, Duration duration) {}

  public static List<Duration> totalDurationOfMovieBatch(Stream<Movie> movies, int batchSize) {
    return movies
        .filter(movie -> movie.rating() >= 3)
        .gather(Gatherers.windowFixed(batchSize))
        .gather(Gatherers.scan(
            () -> Duration.ZERO,
            (avgSoFar, window) -> window.getFirst()
                .duration()
                .plus(window.getLast().duration())
            )
        )
        .peek(d ->
            System.out.printf("Total duration of %d movies: %s hours %s",
                batchSize,d.toHours(), LS))
        .toList();
  }
}
