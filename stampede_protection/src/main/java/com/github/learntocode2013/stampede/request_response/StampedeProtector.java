package com.github.learntocode2013.stampede.request_response;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

public interface StampedeProtector {
    CompletableFuture<DomainResponse> fetchWithProtection(
            String cacheKey,
            Supplier<DomainResponse> valueSupplierFromCache,
            Supplier<DomainResponse> valueSupplierOnCacheMiss
    );

    Optional<CompletableFuture<DomainResponse>> getInFlightRequestForKey(String cachekey);
}
