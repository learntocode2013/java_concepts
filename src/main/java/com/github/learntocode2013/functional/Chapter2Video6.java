package com.github.learntocode2013.functional;

import java.time.LocalDate;
import java.util.function.Function;

// Demonstrate return function as a result from another function
public class Chapter2Video6 {
    protected static class MyMath {
        public static Integer timesTwo(int x) {
            return createMultiplier(2).apply(x);
        }

        public static Integer timesThree(int x) {
            return createMultiplier(3).apply(x);
        }

        public static Integer timesFour(int x) {
            return createMultiplier(4).apply(x);
        }

        public static Function<Integer, Integer> createMultiplier(int multiplier) {
            return num -> num * multiplier;
        }
    }

    private static void demoBasicIdea() {
        var prefix = "The date is";
        NoArgFunction<NoArgFunction<String>> createGreeter = () -> () -> String.join(" : ",
                prefix,
                LocalDate.now().toString());

        var greeter = createGreeter.apply();
        System.out.println(greeter.apply());
    }

    public static void main(String[] args) {
        Function<Integer, Integer> timesTwo = MyMath.createMultiplier(2);
        Function<Integer, Integer> timesThree = MyMath.createMultiplier(3);
        Function<Integer, Integer> timesFour = MyMath.createMultiplier(4);

        System.out.printf("%d times 2 is : %d %n", 2, timesTwo.apply(2));
        System.out.printf("%d times 3 is : %d %n", 2, timesThree.apply(2));
        System.out.printf("%d times 4 is : %d %n", 2, timesFour.apply(2));
    }
}
