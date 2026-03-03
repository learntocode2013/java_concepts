package com.github.learntocode2013;

public class HelloSum {
    sealed interface Robot
            permits Vacuum, LawnMower { }

    static final class Vacuum implements Robot {
        public void suck() {
            System.out.println("Sucking dirt....");
        }
    }

    static final class LawnMower implements Robot {
        public void cut() {
            System.out.println("Cutting grass....");
        }
    }

    static void robotStuff(Robot robot) {
        // new way(s) - using "pattern match"
        if (robot instanceof Vacuum v) {
            v.suck();
        }

//        switch (robot) {
//            case Vacuum v -> v.suck();
//            case LawnMower lm -> lm.cut();
//        }
    }

    public static void main(String[] args) {
        robotStuff(new Vacuum());
        robotStuff(new LawnMower());
    }
}
