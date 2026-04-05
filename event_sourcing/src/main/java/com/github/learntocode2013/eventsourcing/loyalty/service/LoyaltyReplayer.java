package com.github.learntocode2013.eventsourcing.loyalty.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.learntocode2013.eventsourcing.order.events.DomainEvent;
import com.github.learntocode2013.eventsourcing.order.events.OrderPlaced;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class LoyaltyReplayer {
    private static final Logger logger = Logger.getLogger(LoyaltyReplayer.class.getSimpleName());

    @Autowired
    @Qualifier("clickhouseJdbc")
    private JdbcTemplate clickhouse;
    @Autowired private ObjectMapper mapper;
    @Autowired private KafkaProperties kafkaProperties;

    public void replayHistory() {
        Map<String, Object> consumerProperties = kafkaProperties.buildConsumerProperties();
        consumerProperties.put(ConsumerConfig.GROUP_ID_CONFIG, "loyalty-rebuild-" + UUID.randomUUID());
        consumerProperties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        try(KafkaConsumer<String, DomainEvent> consumer = new KafkaConsumer<>(
                consumerProperties,
                new StringDeserializer(),
                new JsonDeserializer<>(DomainEvent.class, mapper, false))) {
            String topic = "order-events";
            var partitionInfos = consumer.partitionsFor(topic);
            var topicPartitions = partitionInfos.stream()
                    .map(info -> new TopicPartition(topic, info.partition()))
                    .toList();
            consumer.assign(topicPartitions);
            consumer.seekToBeginning(topicPartitions);

            List<Object[]> batchArgs = new ArrayList<>();
            while(true) {
                ConsumerRecords<String, DomainEvent> records = consumer.poll(Duration.ofMillis(1000));
                if (records.isEmpty()) {
                    break;
                }
                for(var record : records) {
                    if (record.value() == null) continue;
                    DomainEvent event = record.value();
                    if (event instanceof OrderPlaced op) {
                        double points = op.amount()/10;
                        batchArgs.add(new Object[]{op.customerId(), points});
                    }
                }
                
                if (!batchArgs.isEmpty()) {
                    clickhouse.batchUpdate("INSERT INTO loyalty_points (customer_id, points) values (?, ?)", batchArgs);
                    batchArgs.clear();
                }
            }

        } catch (Exception e) {
            logger.log(Level.SEVERE, "Failed to replay events to build loyalty state", e);
        }
    }
}
