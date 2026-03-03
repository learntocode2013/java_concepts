package com.github.learntocode2013;

import java.util.Set;

public class HelloNullHelp {
    record Plumbus(String s) { }
    record Thing(Plumbus plumbus) { }
    record MeeSeeks(Thing thing) { }

    public static void main(String[] args) {
        var meeSeeks1 = new MeeSeeks(new Thing(new Plumbus("hello")));
        var meeSeeks2 = new MeeSeeks(new Thing(null));

        // Add to set
        var meSeeks = Set.of(meeSeeks1, meeSeeks2);

        meSeeks.forEach(it -> System.out.println(it.thing.plumbus.s));
    }
}
