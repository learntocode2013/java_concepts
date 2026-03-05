package com.github.learntocode2013;

import static com.github.learntocode2013.GatherersForTelemetryAndRateLimits.analyzeLoginEvents;
import static org.junit.jupiter.api.Assertions.*;

import com.github.learntocode2013.GatherersForTelemetryAndRateLimits.LoginEvent;
import com.github.learntocode2013.GatherersForTelemetryAndRateLimits.SecurityAlertType;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

class GatherersForTelemetryAndRateLimitsTest {
  @Test
  void demo_sliding_window_in_telemetry() {
    var loginEvents = Stream.of(
        new LoginEvent("10.127.10.1", true),
        new LoginEvent("10.127.10.2", false),
        new LoginEvent("10.127.10.2", false),
        new LoginEvent("10.127.10.2", true),
        new LoginEvent("10.127.10.3", false),
        new LoginEvent("10.127.10.3", false),
        new LoginEvent("10.127.10.3", false),
        new LoginEvent("10.127.10.4", true)
    );
    var maybeSecurityAlert = analyzeLoginEvents(loginEvents);
    maybeSecurityAlert.ifPresent(alert -> {
      assertSame(SecurityAlertType.LOGIN_FAILURE, alert.type());
      assertEquals("10.127.10.3",
          maybeSecurityAlert.get().ipAddress());
    });
  }
}