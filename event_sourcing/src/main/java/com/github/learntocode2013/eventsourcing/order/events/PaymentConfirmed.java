package com.github.learntocode2013.eventsourcing.order.events;

import java.time.Instant;
import java.util.UUID;

public record PaymentConfirmed(UUID orderId, Instant occurredAt) implements DomainEvent {
    @Override
    public UUID getId() {
        return orderId();
    }

    @Override
    public Instant getOccurredAt() {
        return occurredAt();
    }
}
