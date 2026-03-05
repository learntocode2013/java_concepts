package com.github.learntocode2013;

import static com.github.learntocode2013.GatherersForBatchIngestion.write_to_db_in_batches;

import com.github.learntocode2013.GatherersForBatchIngestion.UserRecord;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class GatherersForBatchIngestionTest {
  @Test
  void demo_fixed_window_for_batch_publishing() {
    var recordStream  = IntStream.rangeClosed(1, 2500)
        .mapToObj(i -> new UserRecord(
            UUID.randomUUID(),
            "User" + i));
    write_to_db_in_batches(
        recordStream,
        1000);
  }
}