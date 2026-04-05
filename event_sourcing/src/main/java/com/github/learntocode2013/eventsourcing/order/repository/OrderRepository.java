package com.github.learntocode2013.eventsourcing.order.repository;

import com.github.learntocode2013.eventsourcing.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderRepository extends JpaRepository<Order, UUID> {
}
