package com.github.learntocode2013;

import io.vavr.control.Try;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Gatherers;
import java.util.stream.Stream;

/**
 * The Scenario: We need to enrich a stream of User IDs by calling a 3rd-party CRM API.
 * We want to make these network calls concurrently to save time,
 * but the API will rate-limit us if you exceed 5 concurrent requests
 */
public class GatherersForApiThrottling {
  public static final String LS =  System.lineSeparator();
  public record UserProfile(String id, String emailAddr) { }

  public static List<Try<UserProfile>> enrichUserIdViaProfiles(
      Stream<String> idStream,
      int concurrency) {
    return idStream
        .gather(Gatherers.mapConcurrent(
            concurrency,
            GatherersForApiThrottling::fetchProfileFromCRM))
        .toList();
  }
  private static Try<UserProfile> fetchProfileFromCRM(String userId) {
    return Try.of(() -> {
      System.out.printf("Virtual thread: %s | TID: %s | Time: %s | Fetching profile for %s from CRM %s",
          Thread.currentThread().isVirtual(),
          Thread.currentThread(),
          LocalDateTime.now(),
          userId, LS);
      Thread.sleep(Duration.ofSeconds(10).toMillis());
      return new UserProfile(userId, userId + "@gmail.com");
    });
  }
}
