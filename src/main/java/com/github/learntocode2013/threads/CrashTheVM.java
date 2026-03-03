package com.github.learntocode2013.threads;

import java.time.Duration;
import java.util.ArrayList;

public class CrashTheVM {
    private static void looper(int count) {
        var tid = Thread.currentThread().getName();
        if(count > 500) {
            return;
        }

        try {
            Thread.sleep(Duration.ofMillis(10).toMillis());
            if(count % 100 == 0) {
                System.out.printf("Thread id: %2d | count: %d %n", tid, count);
            }
        } catch (InterruptedException iex) {
            iex.printStackTrace();
        }
        looper(count + 1);
    }

    public static Thread makeThread(Runnable r, int serialNum) {
        return new Thread(r, "HeavyWeight-"+serialNum);
    }

    public static void main(String[] args) {
        var threads = new ArrayList<Thread>();
        for(int i = 0; i < 20_000; i = i + 1) {
            var t = makeThread(() -> looper(1), i);
            t.start();
            threads.add(t);
            if(i % 1000 == 0) {
                System.out.printf("Thread %d started %n", i);
            }
        }

        threads.forEach(t -> {
            try {
                t.join();
            } catch (InterruptedException iex) {
                iex.printStackTrace();
            }
        });
    }
}
