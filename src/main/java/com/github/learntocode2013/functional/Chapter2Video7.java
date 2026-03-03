package com.github.learntocode2013.functional;

// Closure demonstration
public class Chapter2Video7 {
    public static void main(String[] args) {
        NoArgFunction<NoArgFunction<String>> createGreeter = () -> {
            String name = "Dibakar";
            return () -> String.join(" ", "Hej", name, "!");
        };

        var greeter = createGreeter.apply();
        System.out.println(greeter.apply());
    }
}
