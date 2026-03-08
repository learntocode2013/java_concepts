package com.github.learntocode2013;

import io.vavr.control.Try;
import java.time.Duration;
import java.util.List;
import java.util.stream.Gatherers;
import java.util.stream.Stream;
import org.checkerframework.checker.nullness.qual.NonNull;

public class GathererForOrderProcessing {
  enum Priority {
    STANDARD,
    EXPRESS,
    OVERNIGHT
  }
  public record Order(
      String orderId,
      String warehouseZone,
      int itemCount,
      Priority priority
  ) {}

  public static void processWarehouseOrders(
      Stream<Order> orders,
      int batchSize) {
    orders
        .gather(Gatherers.windowFixed(batchSize))
        .gather(Gatherers.mapConcurrent(4,
            batch -> {
              return Try.of(() -> pickOrders(batch));
            }))
        .filter(Try::isSuccess)
        .map(Try::get)
        .forEach(batch ->
            System.out.printf("Batch completed: %d orders %n", batch.size()));
  }

  private static @NonNull List<Order> pickOrders(List<Order> batch) throws InterruptedException {
    System.out.printf("Picking %d orders from %s zone on thread %s \n",
        batch.size(),
        batch.getFirst().warehouseZone(),
        Thread.currentThread());
    Thread.sleep(Duration.ofSeconds(3).toMillis());
    return batch;
  }
}
