package com.github.learntocode2013;

import java.time.LocalDate;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
// Demonstrate usage of Period class
public class SubscriptionManager
{
    private static final Logger  logger = Logger.getLogger(SubscriptionManager.class.getName());

    public static int calculateAge(LocalDate birthDate) {
        LocalDate now = LocalDate.now();
        var timeOnEarth = Period.between(birthDate, now);
        logger.log(Level.INFO, "Your age is {0} years", timeOnEarth.getYears());
        return timeOnEarth.getYears();
    }

    public static boolean isSubscriptionActive(LocalDate testDate) {
        LocalDate subscriptionStart = LocalDate.of(2025, 6, 17);
        Period subscriptionPeriod = Period.ofMonths(12);
        LocalDate subscriptionEnd = subscriptionStart.plus(subscriptionPeriod);

        if (testDate.isBefore(subscriptionEnd)) {
            long daysRemaining = ChronoUnit.DAYS.between(testDate, subscriptionEnd);
            logger.log(Level.INFO,
                "Your subscription is still active. You have {0} days remaining until expiry",
                daysRemaining);
            return daysRemaining > 0;
        }
        logger.log(Level.INFO, "Your subscription has expired.");
        return false;
    }
}
