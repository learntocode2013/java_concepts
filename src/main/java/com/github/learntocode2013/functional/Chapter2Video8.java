package com.github.learntocode2013.functional;

import java.util.function.BiFunction;
import java.util.function.Function;

// Demonstrate higher order function
public class Chapter2Video8 {
    public static void main(String[] args) {
        BiFunction<Float, Float, Float> divFnc = (x, y) -> x / y;
        // Decorator function
        Function<BiFunction<Float, Float, Float>, BiFunction<Float, Float, Float>> checkDivisorIsntZero =
                fnc -> (x, y) -> {
                    if(y == 0f) {
                        System.err.println("Error: cannot divide by zero !!!");
                        return 0f;
                    }
                    return fnc.apply(x, y);
                };

        var decoratedFnc = checkDivisorIsntZero.apply(divFnc);
        System.out.printf("%f %s %f : %f %n", 10f, "/", 0f,decoratedFnc.apply(10f, 0f));
        System.out.printf("%f %s %f : %f %n", 10f, "/", 2f, decoratedFnc.apply(10f, 2f));
    }
}
