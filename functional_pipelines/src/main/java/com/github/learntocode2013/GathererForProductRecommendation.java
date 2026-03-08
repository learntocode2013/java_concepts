package com.github.learntocode2013;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Gatherers;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Problem: Update product recommendations based on recent view patterns
 */
public class GathererForProductRecommendation {
  public record ProductView(
      String userId,
      String productId,
      String category,
      LocalDateTime timestamp
  ) {}

  public record Recommendation(
      String userId,
      List<String> recommendedProducts
  ) {}

  public static List<Recommendation> generateRecommendations(
      Stream<ProductView> productViews,
      int batchSize) {
    return productViews.gather(Gatherers.windowSliding(batchSize))
        .filter(window -> window.size() == batchSize)
        .map(GathererForProductRecommendation::recommendationForBatch)
        .toList();
  }

  private static @NonNull Recommendation recommendationForBatch(List<ProductView> window) {
    String userId = window.getFirst().userId();
    Comparator<Map.Entry<String,Long>> sortByCount = Comparator.comparing(Map.Entry::getValue);
    Comparator<Entry<String, Long>> sortByCountDesc = sortByCount.reversed();
    var topCategories = window.stream()
        .collect(Collectors.groupingBy(
            ProductView::category,
            Collectors.counting()))
        .entrySet()
        .stream()
        .sorted(sortByCountDesc)
        .limit(3)
        .map(Entry::getKey)
        .toList();
    return new Recommendation(
        userId,
        topCategories
    );
  }
}
