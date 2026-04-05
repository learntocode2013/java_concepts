package com.github.learntocode2013.eventsourcing.loyalty.projector;

import com.github.learntocode2013.eventsourcing.order.events.DomainEvent;
import com.github.learntocode2013.eventsourcing.order.events.OrderPlaced;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.logging.Logger;

@Service
public class LoyaltyProjector {
    private static final Logger logger = Logger.getLogger(LoyaltyProjector.class.getSimpleName());

    @Autowired
    @Qualifier("clickhouseJdbc")
    private JdbcTemplate clickhouse;

    @KafkaListener(topics = "order-events", groupId = "loyalty-group")
    public void consumeDomainEvent(DomainEvent event) {
        if (event instanceof OrderPlaced op) {
            logger.info("Projecting to Loyalty: " + op);
            double points = op.amount() / 10.0;
            // SummingMergeTree in ClickHouse handles aggregation by summing new points into existing totals
            // based on customer_id. INSERT is the idiomatic way to 'update' sum in this engine.
            clickhouse.update("INSERT INTO loyalty_points (customer_id, points) values (?, ?)",
                    op.customerId(),
                    points);
        }
    }
}
