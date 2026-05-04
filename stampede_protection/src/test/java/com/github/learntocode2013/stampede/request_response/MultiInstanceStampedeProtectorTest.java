package com.github.learntocode2013.stampede.request_response;

import io.vavr.control.Try;
import org.junit.jupiter.api.*;
import org.redisson.Redisson;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.github.learntocode2013.stampede.request_response.MultiInstanceStampedeProtector.JOB_STATE_MAP;

// ----------------------------------------------------------------
// NOTE: Execute the docker command below before uncommenting the
// Disabled tag to run the tests.
// docker run -d --name redis-local -p 6379:6379 redis:7.2
// ---------------------------------------------------------------
@Disabled
class MultiInstanceStampedeProtectorTest {
    private static final Logger log = Logger.getLogger(MultiInstanceStampedeProtectorTest.class.getName());
    private static final AtomicInteger DEPENDENCY_INVOCATION_COUNTER = new AtomicInteger(0);
    private static final String CACHE_PREFIX = "catalog:cache:";
    private static final String CACHE_KEY =  CACHE_PREFIX + "2026-05-03";
    private static final String CACHE_VALUE = "CATALOG_FULL";
    private Supplier<DomainResponse> cacheValueSupplier;
    private Supplier<DomainResponse> valueSupplierOnCacheMiss;
    private RedissonClient redisson;


    @BeforeEach
    void setUp() {
        Config config = new Config();
        var serverAddress = String.format("redis://%s:%s",
                "127.0.0.1",
                "6379");
        config.useSingleServer().setAddress(serverAddress);
        redisson = Redisson.create(config);
        cacheValueSupplier = () -> {
            RBucket<DomainResponse> cache = redisson.getBucket(CACHE_KEY);
            return cache.get();
        };
        valueSupplierOnCacheMiss = () -> fetchFromCatalogService("2026-05-03", CACHE_KEY).getOrNull();
        DEPENDENCY_INVOCATION_COUNTER.set(0);
        RBucket<DomainResponse> cache = redisson.getBucket(CACHE_KEY);
        cache.delete();
        redisson.getMap(JOB_STATE_MAP).clear();
    }

    @AfterEach
    void tearDown() {
        RBucket<DomainResponse> cache = redisson.getBucket(CACHE_KEY);
        cache.delete();
        DEPENDENCY_INVOCATION_COUNTER.set(0);
        redisson.shutdown();
    }

    @Test
    void verifyStampedeProtection_AcrossServiceInstances_OnCacheMiss() {
        var taskCount = Runtime.getRuntime().availableProcessors();
        List<CompletableFuture<DomainResponse>> futures = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch countDownLatch = new CountDownLatch(taskCount);
        try(ExecutorService es = Executors.newFixedThreadPool(taskCount)) {
            for (int i = 0; i < taskCount; i++) {
                es.execute(() -> {
                    StampedeProtector protector = new MultiInstanceStampedeProtector(redisson);
                    var resultCF = protector.fetchWithProtection(
                            CACHE_KEY,
                            cacheValueSupplier,
                            valueSupplierOnCacheMiss);
                    futures.add(resultCF);
                    countDownLatch.countDown();
                });
            }
            countDownLatch.await();
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

            for(var future : futures) {
                var resp = future.get();
                Assertions.assertEquals("COMPLETED", resp.status(),
                        "All requests should eventually complete");
                Assertions.assertEquals(CACHE_VALUE, resp.responseBody(),
                        "All request should have the hydrated data");
            }
        } catch (Exception ex) {
            log.log(Level.SEVERE, ex.getMessage(), ex);
        }
        Assertions.assertEquals(1, DEPENDENCY_INVOCATION_COUNTER.get());
    }

    @Test
    void verifyStampedeProtection_AcrossServiceInstances_OnCacheHit() {
        fetchFromCatalogService("2026-05-03", CACHE_KEY).getOrNull();
        DEPENDENCY_INVOCATION_COUNTER.set(0);
        int taskCount = Runtime.getRuntime().availableProcessors();
        List<CompletableFuture<DomainResponse>> futures = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch countDownLatch = new CountDownLatch(taskCount);
        try(ExecutorService es = Executors.newFixedThreadPool(taskCount)) {
            for (int i = 0; i < taskCount; i++) {
                es.execute(() -> {
                    StampedeProtector protector = new MultiInstanceStampedeProtector(redisson);
                    var resultCF = protector.fetchWithProtection(
                      CACHE_KEY,
                      cacheValueSupplier,
                      valueSupplierOnCacheMiss
                    );
                    futures.add(resultCF);
                    countDownLatch.countDown();
                });
            }
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            for(var future : futures) {
                var resp = future.get();
                Assertions.assertEquals("COMPLETED", resp.status());
                Assertions.assertEquals(CACHE_VALUE, resp.responseBody());
            }
        } catch (Exception ie) {
            Thread.currentThread().interrupt();
            log.log(Level.SEVERE, ie.toString(), ie);
        }
        Assertions.assertEquals(0, DEPENDENCY_INVOCATION_COUNTER.get());
    }

    private Try<DomainResponse> fetchFromCatalogService(String request, String cacheKey) {
        try {
            log.log(Level.INFO, "{0} fetching from catalog service for request: {1}",
                    new Object[]{Thread.currentThread().getName(),request});
            Thread.sleep(Duration.ofSeconds(5));
            var responseFromDependencySvc = new DomainResponse(CACHE_VALUE, request, "COMPLETED");
            RBucket<DomainResponse> cache = redisson.getBucket(cacheKey);
            cache.set(responseFromDependencySvc, Duration.ofHours(1));
            DEPENDENCY_INVOCATION_COUNTER.incrementAndGet();
            return Try.success(responseFromDependencySvc);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return Try.failure(ie);
        }
    }

}