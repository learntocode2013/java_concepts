package com.github.learntocode2013.functional;

import java.util.function.BiFunction;

public class Chapter2Video5 {
    protected static class MyMath {
        public static Integer add(int x, int y) {
            return x + y;
        }

        public static Integer subtract(int x, int y) {
            return x - y;
        }

        public static Integer combine2And3(BiFunction<Integer, Integer, Integer> combineFnc) {
            return combineFnc.apply(3, 2);
        }
    }
    public static void main(String[] args) {
        System.out.println(MyMath.combine2And3(MyMath::add));
        System.out.println(MyMath.combine2And3(MyMath::subtract));
    }
}
