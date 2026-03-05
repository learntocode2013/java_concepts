package com.github.learntocode2013;

import java.util.List;
import java.util.Optional;
import java.util.stream.Gatherers;
import java.util.stream.Stream;

/**
 * The Scenario: We have a stream of login events. We want to trigger a
 * security alert if you see 3 consecutive failed logins from the same
 * IP address.
 */
public class GatherersForTelemetryAndRateLimits {
  public static final String LS = System.lineSeparator();
  enum SecurityAlertType {
    LOGIN_FAILURE
  }
  public record LoginEvent(String ipAddress, boolean isSuccess) {}
  public record SecurityAlert(
      SecurityAlertType type,
      String ipAddress,
      int consecutiveFailures) {}

  public static Optional<SecurityAlert> analyzeLoginEvents(
      Stream<LoginEvent> events) {
    return events
        .gather(Gatherers.windowSliding(3))
        .peek(window ->
            System.out.printf("Window --> %s %s %s", window, LS, LS))
        .filter(window -> window.size() == 3)
        .filter(GatherersForTelemetryAndRateLimits::fromSameIpAddress)
        .map(window -> new SecurityAlert(
            SecurityAlertType.LOGIN_FAILURE,
            window.getFirst().ipAddress(),
            window.size())
        )
        .findFirst();
  }

  private static boolean fromSameIpAddress(List<LoginEvent> window) {
    return window.stream()
        .map(e -> e.ipAddress)
        .distinct()
        .count() == 3;
  }
}
