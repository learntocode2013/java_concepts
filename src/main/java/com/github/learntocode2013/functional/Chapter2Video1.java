package com.github.learntocode2013.functional;

import java.time.LocalDate;
import java.util.function.BiFunction;
import java.util.function.Function;

public class Chapter2Video1 {
    protected static class MyMath {
        public static Integer triple(Integer num) {
            return num * 3;
        }
    }

    public static void main(String[] args) {
        Function<Integer, Integer> myTriple = MyMath::triple;
        System.out.printf("Triple of: %d is: %d %n", 2, myTriple.apply(2));

        Function<Integer, Integer> absoluteValue = x -> x < 0 ? -x : x;
        var result = absoluteValue.apply(-10);
        System.out.printf("Absolute value of: %d is: %d %n", -10, result);

        BiFunction<Integer, Integer, Integer> adder = Integer::sum;
        result = adder.apply(5, 6);
        System.out.printf("Summation of %d and %d is: %d %n", 5, 6 , result);

        TriFunction<Integer, Integer, Integer, Integer> addThree = (x, y, z) -> x + y + z;
        result = addThree.apply(2, 3, 5);
        System.out.printf("Summation of %d, %d and %d is: %d %n", 2, 3, 5, result);

        NoArgFunction<String> currentYear = () -> "May you have a successful " + LocalDate.now().getYear();
        System.out.printf("%s %n", currentYear.apply());
    }
}
