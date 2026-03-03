package com.github.learntocode2013;

import java.time.LocalDate;
import java.time.Month;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class DemoPeriodTest {
  @Test
  void testSubscription() {
    boolean isSubscriptionActive = SubscriptionManager.isSubscriptionActive(LocalDate.now());
    Assertions.assertTrue(isSubscriptionActive);
  }

  @Test
  void testAgeCorrectness() {
    var age = SubscriptionManager.calculateAge(LocalDate.of(1984, Month.FEBRUARY,20));
    Assertions.assertEquals(41, age);
  }
}