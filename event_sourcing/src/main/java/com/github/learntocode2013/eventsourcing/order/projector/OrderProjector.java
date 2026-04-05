package com.github.learntocode2013.eventsourcing.order.projector;

import com.github.learntocode2013.eventsourcing.order.events.DomainEvent;
import com.github.learntocode2013.eventsourcing.order.events.OrderPlaced;
import com.github.learntocode2013.eventsourcing.order.events.PaymentConfirmed;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.logging.Logger;

@Service
public class OrderProjector {
    private static final Logger logger = Logger.getLogger(OrderProjector.class.getSimpleName());

    @Autowired
    @Qualifier("clickhouseJdbc")
    private JdbcTemplate clickhouse;

    @KafkaListener(topics = "order-events", groupId = "live-dashboard-group")
    public void consumeDomainEvent(DomainEvent event) {
        logger.info("Projecting domain event: " + event);
        if (event instanceof OrderPlaced op) {
            clickhouse.update("INSERT INTO order_dashboard (id, amount, status) values (?, ?, ?)",
                    op.orderId(),
                    op.amount(),
                    "PLACED");
        } else if (event instanceof PaymentConfirmed pc) {
            clickhouse.update("ALTER TABLE order_dashboard UPDATE status = ? WHERE id = ?",
                    "PAID",
                    pc.orderId());
        }
    }
}
