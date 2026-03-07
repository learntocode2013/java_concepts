package com.github.learntocode2013;

import com.github.learntocode2013.GathererForTrading.Tick;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Month;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Gatherer;
import java.util.stream.Gatherers;
import java.util.stream.IntStream;
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
  void demo_fraud_detection_using_sliding_window() {
    var transactions = Stream.of(
        new Transaction("5631-6653-3321-9891",
            BigDecimal.valueOf(100_000),
            "Bengaluru",
            LocalDateTime.of(2026, Month.MARCH,
                7,20, 20, 20)),
        new Transaction("5631-6653-3321-9891",
            BigDecimal.valueOf(100_000),
            "Pune",
            LocalDateTime.of(2026, Month.MARCH,
                7,20, 24, 20)),
        new Transaction("5631-6653-3321-9891",
            BigDecimal.valueOf(100_000),
            "Delhi",
            LocalDateTime.of(2026, Month.MARCH,
                7,20, 25, 20)),
        new Transaction("5631-6653-3321-9891",
            BigDecimal.valueOf(10_000),
            "Bengaluru",
            LocalDateTime.of(2026, Month.MARCH,
                7,21, 20, 20))

    );
    int batchSize = 3;
    var maybeFraudAlert = GathererForFraudDetection.detectVelocityFraud(transactions, batchSize);
    maybeFraudAlert.ifPresent(alert -> {
      Assertions.assertEquals(batchSize, alert.suspiciousTransactions().size());
      System.out.println(alert.reason());
      System.out.println("Press `1` to confirm genuine | Press `2` to report fraud");
      alert.suspiciousTransactions().forEach(System.out::println);
    });
  }

  @Test
  void demo_expense_anomaly_using_sliding_window() {
    var transactions = Stream.of(
        new Transaction("5631-6653-3321-9891",
            BigDecimal.valueOf(10_000),
            "Bengaluru",
            LocalDateTime.of(2026, Month.MARCH,
                7,20, 20, 20)),
        new Transaction("5631-6653-3321-9891",
            BigDecimal.valueOf(10_000),
            "Pune",
            LocalDateTime.of(2026, Month.MARCH,
                7,20, 24, 20)),
        new Transaction("5631-6653-3321-9891",
            BigDecimal.valueOf(10_000),
            "Delhi",
            LocalDateTime.of(2026, Month.MARCH,
                7,20, 25, 20)),
        new Transaction("5631-6653-3321-9891",
            BigDecimal.valueOf(10_000),
            "Bengaluru",
            LocalDateTime.of(2026, Month.MARCH,
                7,21, 20, 20)),
        new Transaction("5631-6653-3321-9891",
            BigDecimal.valueOf(10_000),
            "Bengaluru",
            LocalDateTime.of(2026, Month.MARCH,
                7,22, 20, 20)),
        new Transaction("5631-6653-3321-9891",
            BigDecimal.valueOf(10_000),
            "Bengaluru",
            LocalDateTime.of(2026, Month.MARCH,
                7,23, 20, 20)),
        new Transaction("5631-6653-3321-9891",
            BigDecimal.valueOf(10_000),
            "Bengaluru",
            LocalDateTime.of(2026, Month.MARCH,
                8,21, 20, 20)),
        new Transaction("5631-6653-3321-9891",
            BigDecimal.valueOf(10_000),
            "Bengaluru",
            LocalDateTime.of(2026, Month.MARCH,
                8,22, 20, 20)),
        new Transaction("5631-6653-3321-9891",
            BigDecimal.valueOf(10_000),
            "Bengaluru",
            LocalDateTime.of(2026, Month.MARCH,
                7,23, 20, 20)),
        new Transaction("5631-6653-3321-9891",
            BigDecimal.valueOf(10_000),
            "Bengaluru",
            LocalDateTime.of(2026, Month.MARCH,
                9,23, 20, 20)),
        new Transaction("5631-6653-3321-9891",
            BigDecimal.valueOf(1_00_000),
            "Bengaluru",
            LocalDateTime.of(2026, Month.MARCH,
                10,21, 20, 20))

    );
    int batchSize = 10;
    var anomalies = GathererForExpenseAnomaly.detectExpenseAnomalies(
        transactions,
        batchSize);
    Assertions.assertFalse(anomalies.isEmpty());
    System.out.println("Your current spend exceeds your average spending pattern for the last 10 transactions !!!");
    anomalies.forEach(System.out::println);
  }

  @Test
  void demo_trade_window_aggregation() {
    var tickerMovements = IntStream.rangeClosed(1,1000)
        .mapToObj(i -> new Tick(
            "TWLO",
            BigDecimal.valueOf(100+i),
            100*i,
            LocalDateTime.now())
        );
    int aggregateSize = 100;
    var ohlcBars = GathererForTrading.ticksToOHLCBar(tickerMovements, 100);
    Assertions.assertEquals(1000/aggregateSize, ohlcBars.size());
    ohlcBars.forEach(System.out::println);
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
