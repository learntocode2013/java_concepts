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
    private static final ConcurrentHashMap<String, CompletableFuture<DomainResponse>> inFlightRequests =
            new ConcurrentHashMap<>();

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
        var resultCF = inFlightRequests.computeIfAbsent(cacheKey, k -> {
           CompletableFuture<DomainResponse> newRequestCF = new CompletableFuture<>();
           var value = valueSupplierFromCache.get();
           if (Objects.nonNull(value)) {
               log.log(Level.INFO, "Thread: {0} now found a value for cache key: {1} in second try",
                        new Object[]{Thread.currentThread().getName(), cacheKey});
               return CompletableFuture.completedFuture(value);
            }
           CompletableFuture.supplyAsync(valueSupplierOnCacheMiss)
                   .whenComplete((domainResponse, error) -> {
                       if (Objects.nonNull(error)) {
                           log.log(Level.WARNING, "Failed to hydrate cache for key: {0}", cacheKey);
                           newRequestCF.completeExceptionally(error);
                       } else {
                           log.log(Level.INFO, "{0} - Cache was hydrated with value: {1} for key: {2}",
                                   new Object[]{Thread.currentThread().getName(), domainResponse, cacheKey});
                           newRequestCF.complete(domainResponse);
                       }
                   });
           return newRequestCF;
        });
        resultCF.whenComplete((domainResponse, error) -> inFlightRequests.remove(cacheKey));
        return resultCF;
    }

    @Override
    public Optional<CompletableFuture<DomainResponse>> getInFlightRequestForKey(String cachekey) {
        return Optional.ofNullable(inFlightRequests.get(cachekey));
    }
}
