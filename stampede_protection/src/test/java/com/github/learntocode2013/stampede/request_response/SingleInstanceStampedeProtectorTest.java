package com.github.learntocode2013.stampede.request_response;

import io.vavr.control.Try;
import lombok.SneakyThrows;
import org.junit.jupiter.api.*;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SingleInstanceStampedeProtectorTest {
    private static final Logger log = Logger.getLogger(SingleInstanceStampedeProtectorTest.class.getName());
    private static final String CACHE_KEY = "catalog-state-2026-05-01";
    private static final ConcurrentHashMap<String, DomainResponse> RESPONSE_CACHE = new ConcurrentHashMap<>();
    private static final StampedeProtector protector = new SingleInstanceStampedeProtector();
    private static final Supplier<DomainResponse> valueSupplierFromCache = () -> RESPONSE_CACHE.get("catalog-state-2026-05-01");
    private static final Supplier<DomainResponse> valueSupplierOnCacheMiss = () -> fetchFromCatalogService("2026-05-01", CACHE_KEY)
            .getOrNull();
    private static final AtomicInteger DEPENDENCY_INVOCATION_COUNTER = new AtomicInteger(0);

    @BeforeAll
    static void setUp() {
    }

    @AfterAll
    static void tearDown() {
        RESPONSE_CACHE.clear();
        DEPENDENCY_INVOCATION_COUNTER.set(0);
    }

    @AfterEach
    void afterEach() {
        DEPENDENCY_INVOCATION_COUNTER.set(0);
    }

    @Test
    @Order(1)
    @SneakyThrows
    void verifyRequestCoalescingOnCacheMiss() {
        Assertions.assertEquals(0, DEPENDENCY_INVOCATION_COUNTER.get());
        var parallelRequests = Runtime.getRuntime().availableProcessors();
        List<CompletableFuture<DomainResponse>> futures = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(parallelRequests);
        try(ExecutorService executorService = Executors.newFixedThreadPool(parallelRequests)) {
            for(int i = 0; i < parallelRequests; i++) {
                executorService.execute(() -> {
                    var resultCF = protector.fetchWithProtection(
                            CACHE_KEY,
                            valueSupplierFromCache,
                            valueSupplierOnCacheMiss
                    );
                    futures.add(resultCF);
                    latch.countDown();
                });
            }
            latch.await();
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (Exception ex) {
            log.log(Level.SEVERE, null, ex);
        }
        assertEquals(1,  DEPENDENCY_INVOCATION_COUNTER.get());
    }

    @Test
    @Order(2)
    @SneakyThrows
    void verifyNoInflightRequests_OnCacheHit() {
        Assertions.assertEquals(0, DEPENDENCY_INVOCATION_COUNTER.get());
        var parallelRequests = Runtime.getRuntime().availableProcessors();
        List<CompletableFuture<DomainResponse>> futures = Collections.synchronizedList(new ArrayList<>());
        try(ExecutorService executorService = Executors.newFixedThreadPool(parallelRequests)) {
            for(int i = 0; i < parallelRequests; i++) {
                executorService.execute(() -> {
                    var resultCF = protector.fetchWithProtection(
                            CACHE_KEY,
                            valueSupplierFromCache,
                            valueSupplierOnCacheMiss
                    );
                    futures.add(resultCF);
                });

            }
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            Assertions.assertTrue(protector.getInFlightRequestForKey(CACHE_KEY).isEmpty());
        } catch (Exception ex) {
            log.log(Level.SEVERE, null, ex);
        }
        assertEquals(0,  DEPENDENCY_INVOCATION_COUNTER.get());
    }

    private static Try<DomainResponse> fetchFromCatalogService(String request, String cacheKey) {
        try {
            log.log(Level.INFO, "{0} fetching from catalog service via request: {1}",
                    new Object[]{Thread.currentThread().getName(),request});
            Thread.sleep(Duration.ofSeconds(5));
            var responseFromDependencySvc = new DomainResponse("CATALOG_FULL", request);
            RESPONSE_CACHE.put(cacheKey, responseFromDependencySvc);
            DEPENDENCY_INVOCATION_COUNTER.incrementAndGet();
            return Try.success(responseFromDependencySvc);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            return Try.failure(ie);
        }
    }
}