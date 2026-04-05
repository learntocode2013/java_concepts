This comprehensive guide provides the complete technical and operational steps to transition from a state-based monolith to a fully decoupled **Event Sourcing** architecture. [cite_start]In this exercise, a single Spring Boot service handles the entire lifecycle: persisting state in **PostgreSQL**, publishing domain events to **Kafka**, and projecting those events into **ClickHouse** read models[cite: 559, 560, 561].

---

# **Hands-On Exercise: Transitioning to Event Sourcing**

## **1. Infrastructure Setup: Docker Compose**
[cite_start]**Reasoning:** Event sourcing requires an immutable log (Kafka) and specialized storage for snapshots (Postgres) and read models (ClickHouse)[cite: 16, 581, 582, 651]. [cite_start]By using Kafka with high retention, you preserve a "narrative" of every change[cite: 486].

**`docker-compose.yml`**
```yaml
services:
  postgres: # Snapshot & Command Store
    image: postgres:15
    environment:
      - POSTGRES_USER=user
      - POSTGRES_PASSWORD=pass
      - POSTGRES_DB=order_db
    ports: ["5432:5432"]

  kafka: # Immutable Event Log
    image: bitnami/kafka:latest
    environment:
      - KAFKA_CFG_NODE_ID=1
      - KAFKA_CFG_PROCESS_ROLES=controller,broker
      - KAFKA_CFG_CONTROLLER_QUORUM_VOTERS=1@kafka:9093
      - KAFKA_CFG_LISTENERS=PLAINTEXT://:9092,CONTROLLER://:9093
      - KAFKA_CFG_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092
    ports: ["9092:9092"]

  clickhouse: # Read Model Store
    image: clickhouse/clickhouse-server:latest
    ports: ["8123:8123"]
```

---

## **2. Database & Kafka Initialization**
[cite_start]**Reasoning:** We must prepare schemas for the "Command" state and specialized "Read" projections[cite: 550, 730].

### **2.1 Initialize PostgreSQL (Command Store)**
```bash
docker exec -it $(docker ps -qf "name=postgres") psql -U user -d order_db -c "
CREATE TABLE orders (
    id UUID PRIMARY KEY,
    customer_id VARCHAR(255),
    amount DECIMAL,
    status VARCHAR(50)
);"
```

### **2.2 Initialize ClickHouse (Read Models)**
[cite_start]**Reasoning:** We use specialized engines like `SummingMergeTree` to handle background aggregations for the Loyalty model[cite: 745].
```bash
docker exec -it $(docker ps -qf "name=clickhouse") clickhouse-client --query "
CREATE TABLE order_dashboard (id UUID, amount Float64, status String, updated_at DateTime DEFAULT now()) 
ENGINE = ReplacingMergeTree(updated_at) ORDER BY id;

CREATE TABLE loyalty_points (customer_id String, points Float64, updated_at DateTime DEFAULT now()) 
ENGINE = SummingMergeTree() ORDER BY customer_id;"
```

### **2.3 Create Kafka Topic & Verify**
```bash
# Create Topic
docker exec -it $(docker ps -qf "name=kafka") /opt/bitnami/kafka/bin/kafka-topics.sh \
--create --topic order-events --bootstrap-server localhost:9092

# Command to watch raw events in real-time
docker exec -it $(docker ps -qf "name=kafka") /opt/bitnami/kafka/bin/kafka-console-consumer.sh \
--bootstrap-server localhost:9092 --topic order-events --from-beginning
```

---

## **3. Java Implementation (Spring Boot)**

### **3.1 Maven Dependencies (`pom.xml`)**
```xml
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-data-jpa</artifactId>
    </dependency>
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka</artifactId>
    </dependency>
    <dependency>
        <groupId>com.clickhouse</groupId>
        <artifactId>clickhouse-jdbc</artifactId>
        <version>0.6.0</version>
    </dependency>
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
    </dependency>
</dependencies>
```

### **3.2 Application Configuration (`application.yml`)**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/order_db
    username: user
    password: pass
  clickhouse:
    url: jdbc:clickhouse://localhost:8123/default
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
    consumer:
      group-id: event-sourcing-group
      auto-offset-reset: earliest
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"
```

### **3.3 Command Controller & Model Updater**
[cite_start]**Reasoning:** We capture the *intent* of the change as an immutable event to allow for future auditability and replayability[cite: 546, 547].

```java
@RestController
@RequestMapping("/orders")
public class OrderCommandController {
    @Autowired private OrderRepository repository;
    @Autowired private KafkaTemplate<String, Object> kafka;

    @PostMapping
    public void placeOrder(@RequestBody OrderRequest req) {
        UUID id = UUID.randomUUID();
        // Persist Snapshot in Postgres
        repository.save(new OrderEntity(id, req.customerId(), req.amount(), "PLACED"));
        // Emit Domain Event to Kafka
        kafka.send("order-events", id.toString(), new OrderPlaced(id, req.customerId(), req.amount()));
    }
}

@Service
public class OrderProjector {
    @Autowired private JdbcTemplate clickhouse;

    @KafkaListener(topics = "order-events")
    public void project(OrderPlaced event) {
        // Real-time projection to ClickHouse
        clickhouse.update("INSERT INTO order_dashboard (id, amount, status) VALUES (?, ?, ?)", 
            event.orderId(), event.amount(), "PLACED");
    }
}
```

---

## **4. Event Replay Implementation**
[cite_start]**Reasoning:** The `replayHistory()` function creates a temporary consumer to scan the entire immutable log from Offset 0, allowing us to build new read models (like Loyalty) from historical data[cite: 562, 566, 631].

```java
@Service
public class LoyaltyReplayer {
    @Autowired private JdbcTemplate clickhouse;
    @Autowired private KafkaProperties kafkaProperties;
    @Autowired private ObjectMapper objectMapper;

    public void replayHistory() {
        Map<String, Object> props = kafkaProperties.buildConsumerProperties();
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "loyalty-rebuild-" + UUID.randomUUID());
        
        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            TopicPartition partition = new TopicPartition("order-events", 0);
            consumer.assign(Collections.singletonList(partition));
            consumer.seekToBeginning(Collections.singletonList(partition));

            while (true) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));
                if (records.isEmpty()) break; // Replay caught up to current head

                for (ConsumerRecord<String, String> record : records) {
                    OrderPlaced event = objectMapper.readValue(record.value(), OrderPlaced.class);
                    double points = event.amount() / 10.0;
                    // Rebuild the new Loyalty read model
                    clickhouse.update("INSERT INTO loyalty_points (customer_id, points) VALUES (?, ?)",
                        event.customerId(), points);
                }
            }
        } catch (Exception e) { /* Handle log error */ }
    }
}
```

### **Summary of the Transition**
* [cite_start]**Single Service**: A single Spring Boot service handles transactional writes to Postgres and analytical projections to ClickHouse[cite: 19].
* [cite_start]**History Preservation**: Kafka preserves every business event indefinitely, providing a full historical record[cite: 557, 646].
* [cite_start]**Flexibility**: New read models can be derived as needed by replaying the event log, ensuring the system is future-proof[cite: 550, 634, 648].