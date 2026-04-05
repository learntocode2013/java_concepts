package com.github.learntocode2013.eventsourcing.order.repository;

import com.github.learntocode2013.eventsourcing.order.entity.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface OutboxRepository extends JpaRepository<OutboxEvent, UUID> {
    List<OutboxEvent> findByProcessedFalse();
}
