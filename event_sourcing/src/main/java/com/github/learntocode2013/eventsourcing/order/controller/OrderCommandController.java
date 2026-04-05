package com.github.learntocode2013.eventsourcing.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.learntocode2013.eventsourcing.order.entity.Order;
import com.github.learntocode2013.eventsourcing.order.entity.OutboxEvent;
import com.github.learntocode2013.eventsourcing.order.events.OrderPlaced;
import com.github.learntocode2013.eventsourcing.order.repository.OrderRepository;
import com.github.learntocode2013.eventsourcing.order.repository.OutboxRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
@RequestMapping("/orders")
public class OrderCommandController {
    private static final Logger logger = Logger.getLogger(OrderCommandController.class.getSimpleName());

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OutboxRepository outboxRepository;
    @Autowired
    private ObjectMapper mapper;

    @PostMapping
    @Transactional("postgresTransactionManager")
    public ResponseEntity<Order> placeOrder(@RequestBody OrderRequest request) {
        Order newOrder = new Order();
        newOrder.setCustomerId(request.customerId());
        newOrder.setAmount(request.amount());
        newOrder.setStatus("PLACED");
        Order savedOrder = orderRepository.save(newOrder);

        try {
            OrderPlaced event = new OrderPlaced(
                    savedOrder.getId(),
                    request.customerId(),
                    request.amount(),
                    Instant.now()
            );

            OutboxEvent outboxEvent = new OutboxEvent();
            outboxEvent.setAggregateId(savedOrder.getId().toString());
            outboxEvent.setEventType("OrderPlaced");
            outboxEvent.setPayload(mapper.writeValueAsString(event));
            outboxEvent.setCreatedAt(Instant.now());
            outboxEvent.setProcessed(false);
            
            outboxRepository.save(outboxEvent);
            logger.log(Level.INFO, "Saved order and outbox event for order: {0}", savedOrder.getId());
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to save outbox event for order: {0}", savedOrder.getId());
            throw new RuntimeException("Transaction rolled back because event serialization failed", e);
        }

        return ResponseEntity.ok(savedOrder);
    }
}
