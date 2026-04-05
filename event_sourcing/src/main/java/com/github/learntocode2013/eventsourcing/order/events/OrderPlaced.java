package com.github.learntocode2013.eventsourcing.order.events;

import java.time.Instant;
import java.util.UUID;

public record OrderPlaced(UUID orderId, String customerId, Double amount, Instant occurredAt) implements DomainEvent {
    public UUID getId() {
        return orderId();
    }

    @Override
    public Instant getOccurredAt() {
        return occurredAt();
    }
}
