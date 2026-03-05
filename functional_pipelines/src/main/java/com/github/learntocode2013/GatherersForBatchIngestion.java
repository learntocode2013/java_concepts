package com.github.learntocode2013;

import io.vavr.control.Try;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Gatherers;
import java.util.stream.Stream;
import lombok.SneakyThrows;

/**
 * The Scenario: We are migrating 10,000 user records from a CSV to
 * an external database. Inserting them one by one is too slow.
 * We want to chunk them into batches of 1,000 and use a bulk-insert API.
 */
public class GatherersForBatchIngestion {
  public static final String LS = System.lineSeparator();
  public record UserRecord(UUID id, String name) { }

  public static void write_to_db_in_batches(
      Stream<UserRecord> recordStream,
      int batchSize) {
    recordStream
        .gather(Gatherers.windowFixed(batchSize))
        .forEach(GatherersForBatchIngestion::save_to_db);
  }

  @SneakyThrows
  private static void save_to_db(List<UserRecord> recordBatch) {
    Try.of(() -> {
      LocalDateTime start = LocalDateTime.now();
      Thread.sleep(Duration.ofSeconds(recordBatch.size()/500).toMillis());
      Duration elapsedTime = Duration.between(start, LocalDateTime.now());
      System.out.printf("Saved %d records to DB in %d seconds. %s",
          recordBatch.size(),
          elapsedTime.getSeconds(),
          LS);
      return true;
    }).onFailure(e -> System.out.println(e.getMessage()));
  }
}
