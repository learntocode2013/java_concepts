package com.github.learntocode2013.eventsourcing.order.events;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.time.Instant;
import java.util.UUID;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = OrderPlaced.class, name = "OrderPlaced"),
        @JsonSubTypes.Type(value = PaymentConfirmed.class, name = "PaymentConfirmed")
})
public interface DomainEvent {
    UUID getId();
    Instant getOccurredAt();
}

