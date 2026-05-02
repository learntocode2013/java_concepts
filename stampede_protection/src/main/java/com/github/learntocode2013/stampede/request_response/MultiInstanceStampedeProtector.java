package com.github.learntocode2013.stampede.request_response;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public class MultiInstanceStampedeProtector implements StampedeProtector {
    @Override
    public CompletableFuture<DomainResponse> fetchWithProtection(
            String cacheKey,
            Supplier<DomainResponse> valueSupplierFromCache,
            Supplier<DomainResponse> valueSupplierOnCacheMiss) {
        return CompletableFuture.failedFuture(new UnsupportedOperationException());
    }

    @Override
    public Optional<CompletableFuture<DomainResponse>> getInFlightRequestForKey(String cachekey) {
        return Optional.empty();
    }
}
