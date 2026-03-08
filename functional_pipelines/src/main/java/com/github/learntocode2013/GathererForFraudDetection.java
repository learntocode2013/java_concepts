package com.github.learntocode2013;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.stream.Gatherers;
import java.util.stream.Stream;

/**
 * Problem: Detect when a card is used in multiple distant locations within minutes (impossible travel)
 */
public class GathererForFraudDetection {
  private static final String FRAUD_WARNING = "Detected 3 "
      + "transactions in different locations in a span of less than 5 mins";

  public record FraudAlert(
      String cardNumber,
      List<Transaction> suspiciousTransactions,
      String reason
  ) {}

  // Credit Card Fraud Detection - Velocity Check
  public static Optional<FraudAlert> detectVelocityFraud(
      Stream<Transaction> transactions,
      int batchSize
  ) {
      return transactions
          .gather(Gatherers.windowFixed(batchSize))
          .filter(window -> window.size() == 3)
          .filter(GathererForFraudDetection::isVelocityFraudLikely)
          .map(window -> new FraudAlert(
              window.getFirst().cardNumber(),
              window,
              FRAUD_WARNING
          ))
          .findFirst();
  }

  private static boolean isVelocityFraudLikely(
      List<Transaction> transactions
  ) {
    var firstTransactionTime = transactions.getFirst().dateTime();
    var lastTransactionTime = transactions.getLast().dateTime();
    if (Duration.between(firstTransactionTime, lastTransactionTime).toMinutes() > 5) {
      return false;
    }
    return transactions
        .stream()
        .map(Transaction::merchantLocation)
        .distinct()
        .count() == 3;
  }
}
