package com.github.learntocode2013.cqrsmodelupdater;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.logging.Logger;

@RestController
@RequestMapping("/orders")
public class OrderCommandController {
    private final Logger logger = Logger.getLogger(OrderCommandController.class.getSimpleName());
    private final OrderRepository repository;

    public OrderCommandController(OrderRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public ResponseEntity<Order> createOrder(@RequestBody OrderRequest request) {
        var order = new Order();
        order.setCustomerId(request.getCustomerId());
        order.setAmount(request.getAmount());
        order.setStatus("PENDING");
        return ResponseEntity.ok(repository.saveAndFlush(order));
    }

    @PutMapping("/{orderId}")
    public ResponseEntity<Order> updateOrder(@PathVariable("orderId") java.util.UUID orderId, @RequestBody OrderRequest request) {
        logger.info("Order - " + orderId + " was delivered ? " + request.getDelivered());
        return repository.findById(orderId)
                .map(existingOrder -> {
                    if (Boolean.TRUE.equals(request.getDelivered())) {
                        existingOrder.completeOrder();
                        logger.info("Order - " + orderId + " was completed");
                    }
                    return ResponseEntity.ok(repository.saveAndFlush(existingOrder));
                })
                .orElse(ResponseEntity.notFound().build());
    }

}
