package com.github.learntocode2013.eventsourcing.order.service;

import com.github.learntocode2013.eventsourcing.order.entity.OutboxEvent;
import com.github.learntocode2013.eventsourcing.order.events.DomainEvent;
import com.github.learntocode2013.eventsourcing.order.repository.OutboxRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class OutboxProcessor {
    private static final Logger logger = Logger.getLogger(OutboxProcessor.class.getSimpleName());

    @Autowired
    private OutboxRepository outboxRepository;

    @Autowired
    private KafkaTemplate<String, DomainEvent> kafkaTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Scheduled(fixedDelay = 1000)
    @Transactional("postgresTransactionManager")
    public void processOutbox() {
        List<OutboxEvent> events = outboxRepository.findByProcessedFalse();
        for (OutboxEvent event : events) {
            try {
                DomainEvent domainEvent = objectMapper.readValue(event.getPayload(), DomainEvent.class);
                kafkaTemplate.send("order-events", event.getAggregateId(), domainEvent)
                        .whenComplete((result, ex) -> {
                            if (ex == null) {
                                markAsProcessed(event.getId());
                            } else {
                                logger.log(Level.SEVERE, "Failed to publish event: " + event.getId(), ex);
                            }
                        });
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Error publishing outbox event: " + event.getId(), e);
            }
        }
    }

    @Transactional("postgresTransactionManager")
    public void markAsProcessed(java.util.UUID id) {
        outboxRepository.findById(id).ifPresent(event -> {
            event.setProcessed(true);
            outboxRepository.save(event);
        });
    }
}
