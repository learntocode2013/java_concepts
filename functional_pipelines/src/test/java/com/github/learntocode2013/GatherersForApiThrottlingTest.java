package com.github.learntocode2013;

import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import static com.github.learntocode2013.GatherersForApiThrottling.*;

public class GatherersForApiThrottlingTest {
  @Test
  void test_concurrent_gatherers_for_api_throttling() {
    var idStream = Stream.of(
        "user1",
        "user2",
        "user3",
        "user4",
        "user5",
        "user6",
        "user7");
    var profiles = enrichUserIdViaProfiles(idStream, 5);
    Assertions.assertFalse(profiles.isEmpty());
    Assertions.assertEquals(7, profiles.size());
  }
}