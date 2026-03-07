package com.github.learntocode2013;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Gatherers;
import java.util.stream.Stream;

/**
 * Anomalous Spending Pattern Detection
 * Problem: Flag transactions that are 3x the average of recent spending
 */
public class GathererForExpenseAnomaly {
  public static List<Transaction> detectExpenseAnomalies(
      Stream<Transaction> transactions,
      int batchSize) {
    return transactions
        .gather(Gatherers.windowSliding(batchSize + 1))
        .filter(window -> window.size() == (batchSize + 1))
        .filter(window -> isCurrentExpenseAnAnomaly(
            window,
            batchSize))
        .map(List::getLast)
        .toList();
  }

  private static boolean isCurrentExpenseAnAnomaly(
      List<Transaction> transactions,
      int batchSize
  ) {
      var recentTransactions = transactions.subList(0, batchSize);
      var currentTransaction = transactions.getLast();
      var avgAmount = recentTransactions
          .stream()
          .map(Transaction::amount)
          .gather(Gatherers.fold(() -> BigDecimal.ZERO,BigDecimal::add))
          .reduce(BigDecimal.ZERO,
              (a, b) ->
                  a.add(b).divide(BigDecimal.valueOf(10))
          );
      var threshold = avgAmount.multiply(BigDecimal.valueOf(3));
      System.out.printf("Spending threshold derived from recent patterns: %f%n", threshold);
      return currentTransaction.amount().compareTo(threshold) > 0;
  }
}
