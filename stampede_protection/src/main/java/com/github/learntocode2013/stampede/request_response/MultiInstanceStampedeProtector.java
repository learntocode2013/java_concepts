package com.github.learntocode2013.stampede.request_response;

import org.redisson.api.RCountDownLatch;
import org.redisson.api.RLock;
import org.redisson.api.RMapCache;
import org.redisson.api.RedissonClient;

import java.io.Serializable;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import static java.util.Objects.nonNull;

public class MultiInstanceStampedeProtector implements StampedeProtector {
    private static final Logger logger = Logger.getLogger(MultiInstanceStampedeProtector.class.getName());
    private static final String LOCK_PREFIX = "lock:stampede:";
    public static final String LATCH_PREFIX = "latch:stampede:";
    public static final String JOB_STATE_MAP = "job_states";
    private final RedissonClient redisson;
    public record JobState(String status, String result) implements Serializable {}

    public MultiInstanceStampedeProtector(RedissonClient redissonClient) {
        this.redisson = redissonClient;
    }

    @Override
    public CompletableFuture<DomainResponse> fetchWithProtection(
            String cacheKey,
            Supplier<DomainResponse> valueSupplierFromCache,
            Supplier<DomainResponse> valueSupplierOnCacheMiss) {
        var cachedValue = valueSupplierFromCache.get();
        if (nonNull(cachedValue)) {
            logger.log(Level.INFO, "{0} got a cache hit for {1}",
                    new Object[]{Thread.currentThread().getName(),cacheKey});
            return CompletableFuture.completedFuture(cachedValue);
        }
        logger.log(Level.INFO, "{0} - cache miss for key: {1}",
                new Object[]{Thread.currentThread().getName(),cacheKey});
        return handleCacheMiss(cacheKey, valueSupplierFromCache, valueSupplierOnCacheMiss);
    }

    private CompletableFuture<DomainResponse> handleCacheMiss(
            String cacheKey,
            Supplier<DomainResponse> valueSupplierFromCache,
            Supplier<DomainResponse> valueSupplierOnCacheMiss
    ) {
        RMapCache<String, JobState> jobStates  = redisson.getMapCache(JOB_STATE_MAP);
        var inFlightJob = jobStates.get(cacheKey);
        if (nonNull(inFlightJob)) {
            if ("COMPLETED".equals(inFlightJob.status())) {
                return CompletableFuture.completedFuture(new DomainResponse(
                        inFlightJob.result(),
                        cacheKey,
                        inFlightJob.status()));
            }
            return waitForDistributedJob(cacheKey);
        }
        RLock lock = redisson.getLock(LOCK_PREFIX + cacheKey);
        try {
            if (lock.tryLock(5, 10, TimeUnit.SECONDS)) {
                try {
                    if (!jobStates.containsKey(cacheKey)) {
                        logger.log(Level.INFO, "{0} - Winner instance: Initialized job to fetch and hydrate " +
                                "cache for: {1}", new Object[]{Thread.currentThread().getName(),cacheKey});
                        RCountDownLatch latch = redisson.getCountDownLatch(LATCH_PREFIX + cacheKey);
                        latch.trySetCount(1);
                        var newJobState = new JobState("PROCESSING", null);
                        // Using a 1-minute TTL for the PROCESSING state as a safety measure
                        jobStates.put(cacheKey, newJobState, 1, TimeUnit.MINUTES);
                        return startAsyncCacheHydration(cacheKey, valueSupplierOnCacheMiss);
                    }
                    return waitForDistributedJob(cacheKey);
                } finally {
                    lock.unlock();
                }
            }
            // Could not acquire the lock
            else {
                var value = valueSupplierFromCache.get();
                if (nonNull(value)) {
                    return CompletableFuture.completedFuture(value);
                }
                return waitForDistributedJob(cacheKey);
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return CompletableFuture.completedFuture(new DomainResponse(cacheKey, null, "ERROR"));
        }
    }

    private CompletableFuture<DomainResponse> startAsyncCacheHydration(
            String cacheKey,
            Supplier<DomainResponse> valueSupplierOnCacheMiss) {
        return CompletableFuture.supplyAsync(() -> {
            RMapCache<String, JobState> jobStates = redisson.getMapCache(JOB_STATE_MAP);
            RCountDownLatch latch = redisson.getCountDownLatch(LATCH_PREFIX + cacheKey);
            try {
                var result = valueSupplierOnCacheMiss.get();
                // Use a 5-minute TTL to give all "loser" instances time to read the result
                jobStates.put(cacheKey, new JobState( "COMPLETED", result.responseBody()), 5, TimeUnit.MINUTES);
                logger.log(Level.INFO, "{0} - Job successfully completed for cache key: {1}",
                        new Object[]{Thread.currentThread().getName(),cacheKey});
                return new DomainResponse(
                        result.responseBody(),
                        cacheKey,
                        "COMPLETED");
            } catch (Exception e) {
                logger.severe("Failed to complete job for cache key: " + cacheKey);
                jobStates.remove(cacheKey);
                throw e;
            } finally {
                latch.countDown();
                latch.deleteAsync();
            }
        });
    }

    private CompletableFuture<DomainResponse> waitForDistributedJob(String cacheKey) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                RCountDownLatch latch = redisson.getCountDownLatch(LATCH_PREFIX + cacheKey);
                boolean completed = latch.await(10, TimeUnit.SECONDS);
                if (completed) {
                    RMapCache<String, JobState> jobStates = redisson.getMapCache(JOB_STATE_MAP);
                    JobState existingJobState = jobStates.get(cacheKey);
                    if (existingJobState != null) {
                        return new DomainResponse(
                                existingJobState.result(),
                                cacheKey,
                                existingJobState.status()
                        );
                    }
                }
                return new DomainResponse(
                        null,
                        cacheKey,
                        "TIMED_OUT"
                );
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Wait interrupted", ie);
            }
        });
    }

    @Override
    public Optional<CompletableFuture<DomainResponse>> getInFlightRequestForKey(String cachekey) {
        RMapCache<String, JobState> jobStates = redisson.getMapCache(JOB_STATE_MAP);
        var state = jobStates.get(cachekey);
        if (nonNull(state) && "PROCESSING".equals(state.status())) {
            return Optional.of(waitForDistributedJob(cachekey));
        }
        return Optional.empty();
    }
}
