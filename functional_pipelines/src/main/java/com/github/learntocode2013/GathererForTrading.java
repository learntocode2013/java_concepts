package com.github.learntocode2013;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Gatherers;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.NonNull;

public class GathererForTrading {
  /**
   * Problem: Convert raw tick data into OHLC (Open-High-Low-Close) bars for charting
   */

  public record Tick(
      String symbol,
      BigDecimal price,
      int tradedVolume,
      LocalDateTime timestamp
  ) {}

  public record OHLCBar(
      String symbol,
      BigDecimal open,
      BigDecimal low,
      BigDecimal high,
      BigDecimal close,
      int tradedVolume,
      LocalDateTime barStart
  ) {}

  public static List<OHLCBar> ticksToOHLCBar(
      Stream<Tick> ticks,
      int aggregateSize) {
    return ticks
        .gather(Gatherers.windowFixed(aggregateSize))
        .filter(tickWindow -> tickWindow.size() == aggregateSize)
        .gather(Gatherers.fold(
            () -> new ArrayList<OHLCBar>(),
            (existingLst, currentList) -> {
              var openPrice = currentList.getFirst().price();
              var closingPrice = currentList.getLast().price();
              var highPrice = findHighestRecordedPrice(currentList);
              var lowPrice = findLowestRecordedPrice(currentList, openPrice);
              var totalVolume = computeTotalTradeVolume(currentList);
              existingLst.add(new OHLCBar(
                  currentList.getFirst().symbol(),
                  openPrice,
                  lowPrice,
                  highPrice,
                  closingPrice,
                  totalVolume,
                  currentList.getLast().timestamp()
              ));
              return existingLst;
            }))
        .flatMap(Collection::stream)
        .toList();
  }

  private static @NonNull Integer computeTotalTradeVolume(List<Tick> currentList) {
    return currentList
        .stream()
        .map(Tick::tradedVolume)
        .reduce(0, Integer::sum);
  }

  private static BigDecimal findLowestRecordedPrice(List<Tick> currentList, BigDecimal openPrice) {
    return currentList
        .stream()
        .map(Tick::price)
        .min(Comparator.comparing(BigDecimal::doubleValue))
        .orElse(openPrice);
  }

  private static BigDecimal findHighestRecordedPrice(List<Tick> currentList) {
    return currentList
        .stream()
        .map(Tick::price)
        .max(Comparator.comparing(BigDecimal::doubleValue))
        .orElse(currentList.getLast().price());
  }

}
