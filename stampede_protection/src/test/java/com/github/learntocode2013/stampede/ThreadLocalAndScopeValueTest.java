package com.github.learntocode2013.stampede;

import io.vavr.control.Try;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ThreadLocalAndScopeValueTest {
    private static final Logger LOG =  Logger.getLogger(ThreadLocalAndScopeValueTest.class.getName());
    private static final int MAX_JITTER_SECONDS = 300;

    @BeforeAll
    static void beforeAll() {
        System.setProperty("java.util.secureRandomSeed", "true");
    }

    @Test
    void demo_usage_of_threadLocalRandom() {
        int tasksCount = Runtime.getRuntime().availableProcessors();
        CountDownLatch countDownLatch = new CountDownLatch(tasksCount);
        try(ExecutorService es = Executors.newFixedThreadPool(tasksCount)) {
            for (int i = 0; i < tasksCount; i++) {
                es.submit(() -> {
                    var currentThreadJitterSeconds = ThreadLocalRandom.current().nextInt(MAX_JITTER_SECONDS);
                    var tName = Thread.currentThread().getName();
                    LOG.log(Level.INFO, "{0} will use jitter value: {1}",
                            new Object[]{tName, currentThreadJitterSeconds});
                    countDownLatch.countDown();
                });
            }
        }
        Try.run(countDownLatch::await);
    }
}
