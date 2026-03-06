package com.github.learntocode2013;

import static com.github.learntocode2013.UsingGatherers.totalDurationOfMovieBatch;

import com.github.learntocode2013.UsingGatherers.Movie;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Gatherer;
import java.util.stream.Gatherers;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class BuiltInGathererTest {
  private static final String LS = System.lineSeparator();
  @Test
  @Disabled
  void basicPipeline() {
    var total = Stream.of(1, 2, 4, 5, 6, 3, 7, 8, 9, 10)
        .takeWhile(e -> e != 3)
        .filter(e -> e % 2 == 0)
        .mapToInt(e -> e*2)
        .sum();
    System.out.printf("Total computed by the pipeline: %d%s", total, LS);
  }

  @Test
  @Disabled
  void demo_gatherer_using_custom_map_operation() {
    Stream.of(1, 2, 3, 4)
        .gather(redundantMap())
        .forEach(System.out::println);
  }

  @Test
  @Disabled
  void demo_fold_builtIn_gatherer() {
    Stream.of(1, 2, 4, 5, 6, 3, 7, 8, 9, 10)
        .takeWhile(e -> e != 3)
        .filter(e -> e % 2 == 0)
        .gather(Gatherers.fold(() -> 0, Integer::sum))
        .map(e -> e * 10)
        .forEach(System.out::println);
  }

  @Test
  @Disabled
  void demo_scan_builtIn_gatherer() {
    Stream.of(1, 2, 4, 5, 6, 3, 7, 8, 9, 10)
        .takeWhile(e -> e != 3)
        .filter(e -> e % 2 == 0)
        .gather(Gatherers.scan(() -> 0, Integer::sum))
        .peek(e -> System.out.printf("# Intermediate sum: %d%n", e))
        .max(Comparator.comparingInt(Integer::intValue))
        .stream()
        .map(e -> e * 10)
        .forEach(e -> System.out.printf("** Final sum: %d %s", e, LS));
  }

  @Test
  @Disabled
  void demo_fixed_window_gatherer_builtIn_gatherer() {
    Stream.of(1, 2, 3, 4, 5)
        .gather(Gatherers.windowFixed(3))
        .peek(w -> System.out.printf("# window: %s%n", w))
        .gather(Gatherers.scan(() -> new ArrayList<Integer>(), (a, b) -> {
          a.addAll(b);
          return a;
        }))
        .forEach(System.out::println);
  }

  @Test
  void demo_sliding_window_gatherer_builtIn_gatherer() {
    Stream.of(1, 2, 3, 4, 5)
        .gather(Gatherers.windowSliding(3))
        .peek(w -> System.out.printf("# window: %s%n", w))
        .gather(Gatherers.fold(() -> new ArrayList<Integer>(),  (a, b) -> {
          a.add(b.stream().mapToInt(e -> e).sum());
          return a;
        }))
        .flatMap(Collection::stream)
        .forEach(e ->
            System.out.printf("**Sum of window elems: %d %s", e, LS));
  }

  @Test
  void demo_builtin_gatherer_chaining() {
    var movies = List.of(
        new Movie("Forest Gump", 5, Duration.ofHours(6)),
        new Movie("Shawshank Redemption", 5, Duration.ofHours(4)),
        new Movie("Pursuit to happiness", 4, Duration.ofHours(3)),
        new Movie("Terminator", 4, Duration.ofHours(3)),
        new Movie("StarWars", 3, Duration.ofHours(3))
    );
    int batchSize = 4;
    var durationLst = totalDurationOfMovieBatch(movies.stream(), batchSize);
    var expectedResultSize = ((movies.size() % batchSize) > 0 ? 1 : 0) + movies.size()/batchSize;
    Assertions.assertEquals(expectedResultSize, durationLst.size());
    for(var i = 0; i < durationLst.size(); i++) {
      System.out.printf("Batch: %d | Total watch hours needed: %s %s", i + 1,
          durationLst.get(i).toHours(),
          LS);
    }
  }

  private Gatherer<? super Integer,?, Integer> redundantMap() {
    return Gatherer.of((_,element, downstream) -> {
      if (element != 3) {
        downstream.push(element * 2);
        return true;
      }
      return false; // indication to upstream to send the next element our way
    });
  }
}
