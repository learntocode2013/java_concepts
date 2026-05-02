package com.github.learntocode2013.stampede.request_response;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SingleInstanceStampedeProtector implements StampedeProtector {
    private static final Logger log = Logger.getLogger(SingleInstanceStampedeProtector.class.getName());
    private final ConcurrentHashMap<String, CompletableFuture<DomainResponse>> inFlightRequests =
            new ConcurrentHashMap<>();
    private static final StampedeProtector INSTANCE = new SingleInstanceStampedeProtector();

    private SingleInstanceStampedeProtector() {}

    public static StampedeProtector getInstance() {
        return INSTANCE;
    }

    @Override
    public CompletableFuture<DomainResponse> fetchWithProtection(
            String cacheKey,
            Supplier<DomainResponse> valueSupplierFromCache,
            Supplier<DomainResponse> valueSupplierOnCacheMiss) {
        var cachedValue = valueSupplierFromCache.get();
        if (Objects.nonNull(cachedValue)) {
            log.log(Level.INFO, "Thread: {0} observed a cache hit for {1}",
                    new Object[]{Thread.currentThread().getName(), cacheKey});
            return CompletableFuture.completedFuture(cachedValue);
        }
        log.log(Level.INFO, "Thread: {0} observed a cache miss for {1}",
                new Object[]{Thread.currentThread().getName(), cacheKey});
        var result = inFlightRequests.computeIfAbsent(cacheKey, k -> {
           var value = valueSupplierFromCache.get();
           if (Objects.nonNull(value)) {
               log.log(Level.INFO, "Thread: {0} now found a value for cache key: {1} in second try",
                        new Object[]{Thread.currentThread().getName(), cacheKey});
               return CompletableFuture.completedFuture(value);
            }

           CompletableFuture<DomainResponse> newRequestCF = CompletableFuture.supplyAsync(valueSupplierOnCacheMiss);
           newRequestCF.whenComplete((response, error) -> inFlightRequests.remove(cacheKey, newRequestCF));
           return newRequestCF;
        });

        if (result.isDone()) {
            inFlightRequests.remove(cacheKey, result);
        }
        return result;
    }

    @Override
    public Optional<CompletableFuture<DomainResponse>> getInFlightRequestForKey(String cachekey) {
        return Optional.ofNullable(inFlightRequests.get(cachekey));
    }
}
