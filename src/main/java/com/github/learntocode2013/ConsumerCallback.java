package com.github.learntocode2013;

import java.time.LocalDate;
import java.time.Month;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

public class ConsumerCallback {
    private record Developer(String name, LocalDate joiningDate, AtomicInteger bonus) {}

    public void computeYearlyBonus(LocalDate joiningDate, Function<Integer, Integer> callback) {
        LocalDate targetDate = LocalDate.now(); // the caller does not control this data
        var bonus = callback.apply(targetDate.getDayOfYear() - joiningDate.getDayOfYear() >= 0 ? 1 : 0);
        System.out.printf("Developer is eligible for a bonus of: %d $ %n", bonus);
        // Update payroll system with the bonus amount
    }

    public static void main(String[] args) {
        var creativeDeveloper = new Developer(
                "Dibakar" ,
                LocalDate.of(2022, Month.AUGUST, 1),
                new AtomicInteger(0));
        var testSubject = new ConsumerCallback();
        testSubject.computeYearlyBonus(
                creativeDeveloper.joiningDate(),
                yearsAtJob -> yearsAtJob >= 1 ?  100_000 : 0); // custom logic
    }
}
