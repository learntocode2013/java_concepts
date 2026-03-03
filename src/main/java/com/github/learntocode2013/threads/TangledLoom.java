package com.github.learntocode2013.threads;

import java.util.concurrent.Executors;
import java.util.stream.IntStream;

public class TangledLoom {
    private static int hashing(int length, char c) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.valueOf(c).repeat(Math.max(0, length * 1_000_000)));
        return sb.toString().hashCode();
    }

    public static void main(String[] args) {
        Runnable r = () -> {
            System.out.printf("Thread %s is starting. %n", Thread.currentThread().getName());
            while(true) {
                int total = 0;
                for(int i = 0; i < 10; i++) {
                    total += hashing(i, 'X');
                }
                System.out.printf("Thread id %s | name: %s : %d %n", Thread.currentThread().threadId(), Thread.currentThread().getName(), total);
            }
        };

        var vtFactory = Thread.ofVirtual().factory();
        vtFactory.newThread(r).setName("TangledLoom-");

        try(var scheduler = Executors.newThreadPerTaskExecutor(vtFactory)) {
            IntStream.rangeClosed(0, 10_000).forEach(i -> {
                scheduler.submit(r);
            });
        }
    }
}
