package com.github.learntocode2013.cqrsmodelupdater;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.logging.Logger;

@Service
public class OrderProjectionService {
    private final Logger logger = Logger.getLogger(OrderProjectionService.class.getSimpleName());
    @Autowired
    @Qualifier("clickhouseJdbc")
    private JdbcTemplate clickhouseJdbc;
    @Autowired
    private ObjectMapper mapper;

    @KafkaListener(topics = "order_events.public.orders")
    public void projectToClickHouse(String message) throws Exception {
        logger.info("Received CDC event: \n" + message);
        JsonNode payload = mapper.readTree(message).get("payload").get("after");
        String sql = "INSERT INTO order_summary (id, amount, status) values (?, ?, ?)";
        clickhouseJdbc.update(sql,
                payload.get("id").asText(),
                payload.get("amount").asDouble(),
                payload.get("status").asText()
        );
    }

}
