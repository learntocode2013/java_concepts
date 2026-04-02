This hands-on exercise guide is designed to take you from a standard state-based "Monolithic" model to a fully decoupled **Event Sourcing** architecture. [cite_start]We will use **Spring Boot**, **Kafka** (as the immutable event log), **PostgreSQL** (for snapshots), and **ClickHouse** (for the read model)[cite: 15, 16, 30].

---

## **Part 1: The Infrastructure (Docker Setup)**
[cite_start]**Reasoning:** Event sourcing requires an immutable log that persists "forever" so we can replay history to build new models[cite: 16, 480]. We use Kafka with specific configurations to act as this log.

**`docker-compose.yml`**
```yaml
services:
  kafka:
    image: bitnami/kafka:latest
    environment:
      - KAFKA_CFG_NODE_ID=1
      - KAFKA_CFG_PROCESS_ROLES=controller,broker
      - KAFKA_CFG_CONTROLLER_QUORUM_VOTERS=1@kafka:9093
      - KAFKA_CFG_LISTENERS=PLAINTEXT://:9092,CONTROLLER://:9093
      - KAFKA_CFG_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092
    ports: ["9092:9092"]

  clickhouse: # The Read Model Store
    image: clickhouse/clickhouse-server:latest
    ports: ["8123:8123"]
```

---

## **Part 2: Maven Dependencies**
[cite_start]**Reasoning:** We need `spring-kafka` for the event log interactions and `jackson` to handle polymorphic event serialization (storing different event types in one stream)[cite: 639, 642].

```xml
<dependencies>
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
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>
</dependencies>
```

---

## **Part 3: Defining Immutable Domain Events**
[cite_start]**Reasoning:** In a monolith, we only store the *latest* state[cite: 474, 525]. [cite_start]In event sourcing, we store the *intent*—the individual actions that occurred[cite: 480, 618].

**DomainEvent.java**
```java
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = OrderPlaced.class, name = "OrderPlaced"),
    @JsonSubTypes.Type(value = PaymentConfirmed.class, name = "PaymentConfirmed")
})
public interface DomainEvent {
    UUID getOrderId();
    Instant getOccurredAt();
}

public record OrderPlaced(UUID orderId, String customerId, Double amount, Instant occurredAt) implements DomainEvent {
    public UUID getOrderId() { return orderId; }
    public Instant getOccurredAt() { return occurredAt; }
}
```

---

## **Part 4: The Model Updater (Real-Time Projection)**
[cite_start]**Reasoning:** Because we no longer have a "current state" table in the write DB, we use a Real-Time Model Updater to project events into a Read Database for the UI[cite: 561, 620].

**RealTimeProjector.java**
```java
@Service
public class RealTimeProjector {
    @Autowired private JdbcTemplate clickhouse;

    @KafkaListener(topics = "order-events", groupId = "live-dashboard-group")
    public void consume(DomainEvent event) {
        if (event instanceof OrderPlaced op) {
            // Projecting to a flattened ClickHouse table for speed
            clickhouse.update("INSERT INTO order_dashboard (id, amount, status) VALUES (?, ?, ?)", 
                op.orderId(), op.amount(), "PLACED");
        }
    }
}
```
[cite_start] [cite: 612]

---

## **Part 5: Event Replay (Building a New Read Model)**
**Reasoning:** This is the "Superpower" of event sourcing. [cite_start]If the business asks for a new "Loyalty Points" report six months later, we don't have to worry that we weren't "tracking" points—we just replay all historical events[cite: 562, 626, 627].

**LoyaltyReplayService.java**
```java
@Service
public class LoyaltyReplayService {
    public void rebuildLoyaltyModel() {
        // 1. Create a new, empty Loyalty table
        // 2. Seek Kafka consumer to 'beginning' (offset 0)
        // 3. Process every historical event to calculate points
    }

    @KafkaListener(topics = "order-events", groupId = "loyalty-replay-group", 
                   id = "loyalty-replayer", autoStartup = "false")
    public void replay(DomainEvent event) {
        if (event instanceof PaymentConfirmed pc) {
            // Logic: 1 point for every $10 spent
            updateLoyaltyTable(pc.customerId(), pc.amount() / 10);
        }
    }
}
```
[cite_start] [cite: 612]

---

## **Summary of the Transition**

| Feature | Monolithic (State-Based) | Event Sourcing |
| :--- | :--- | :--- |
| **Primary Data** | [cite_start]Current snapshot only [cite: 474] | [cite_start]Immutable history of changes [cite: 480] |
| **Audit Trail** | [cite_start]Requires separate audit tables [cite: 537] | [cite_start]Native and built-in [cite: 485] |
| **New Reports** | [cite_start]Hard (data might be missing) [cite: 478] | [cite_start]Easy (replay all history) [cite: 627] |
| **Complexity** | [cite_start]Simple but rigid [cite: 8, 34] | [cite_start]Higher operational overhead [cite: 636, 637] |

### **How to Save as PDF**
As an AI, I provide the content in Markdown format. To create the final PDF:
1.  Copy this response into a text editor (e.g., VS Code or Obsidian).
2.  Use a "Markdown to PDF" extension or simply use **Print > Save as PDF** from your browser.
3.  This ensures all code formatting and visualizations remain intact for your team.