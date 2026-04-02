This hands-on exercise integrates the structural separation principles from the provided architecture guide with a full Java implementation[cite: 13, 22]. [cite_start]We will use **Spring Boot**, **PostgreSQL** (Command/Write), **ClickHouse** (Query/Read), and **Debezium** (CDC) to build a resilient, decoupled system[cite: 15, 77].

### **1. Maven Dependencies (`pom.xml`)**
**Reasoning:** We need the Spring Kafka ecosystem to handle the CDC event stream and the respective drivers for our polyglot persistence layer[cite: 19].

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
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <scope>runtime</scope>
    </dependency>
    <dependency>
        <groupId>com.clickhouse</groupId>
        <artifactId>clickhouse-jdbc</artifactId>
        <version>0.6.0</version>
    </dependency>

    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
        <optional>true</optional>
    </dependency>
    <dependency>
        <groupId>com.fasterxml.jackson.core</groupId>
        <artifactId>jackson-databind</artifactId>
    </dependency>
</dependencies>
```

---

### **2. Infrastructure: Docker Compose**
[cite_start]**Reasoning:** To ensure structural separation, the Write and Read models must reside in different specialized data stores[cite: 332, 651].

```yaml
services:
  postgres-write: # Command DB
    image: debezium/postgres:15
    environment:
      - POSTGRES_USER=user
      - POSTGRES_PASSWORD=pass
      - POSTGRES_DB=order_db
    ports: ["5432:5432"]

  kafka: # Event Bus for CDC
    image: bitnami/kafka:latest
    environment:
      - KAFKA_CFG_ADVERTISED_LISTENERS=PLAINTEXT://kafka:9092

  clickhouse-read: # Query DB
    image: clickhouse/clickhouse-server:latest
    ports: ["8123:8123"]
```

---

### **3. The Write Model: Command Side**
[cite_start]**Reasoning:** The write model focuses on domain invariants and transactional safety[cite: 328, 374]. [cite_start]We use a normalized structure to ensure data integrity[cite: 329].

**OrderEntity.java**
```java
@Entity
@Table(name = "orders")
@Data
public class OrderEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;
    private String customerId;
    private Double amount;
    private String status; // PENDING, COMPLETED, CANCELLED

    // Business Logic: Commands enforce rules
    public void completeOrder() {
        if ("PENDING".equals(this.status)) {
            this.status = "COMPLETED";
        }
    }
}
```

---

### **4. Change Data Capture (CDC) Setup**
[cite_start]**Reasoning:** CDC allows us to keep the read model in sync without modifying the write-path code, reducing coupling[cite: 420, 423].

**Step:** Configure Debezium to watch the `orders` table (via POST to Debezium API):
```json
{
  "name": "orders-cdc-connector",
  "config": {
    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
    "database.hostname": "postgres-write",
    "database.dbname": "order_db",
    "table.include.list": "public.orders",
    "topic.prefix": "order_events"
  }
}
```

---

### **5. The Model Updater: Projection**
[cite_start]**Reasoning:** The model updater transforms raw database changes into a denormalized shape optimized for the read-side consumers[cite: 380, 422].

**OrderProjectionService.java**
```java
@Service
public class OrderProjectionService {

    @Autowired
    private JdbcTemplate clickHouseJdbc;

    @KafkaListener(topics = "order_events.public.orders")
    public void projectToReadModel(String message) {
        // Parse the Debezium JSON (simplifying here)
        // Debezium sends "before" and "after" states
        JsonNode payload = objectMapper.readTree(message).get("after");
        
        String sql = "INSERT INTO order_summary (id, amount, status) VALUES (?, ?, ?)";
        clickHouseJdbc.update(sql, 
            payload.get("id").asText(),
            payload.get("amount").asDouble(),
            payload.get("status").asText()
        );
    }
}
```

---

### **6. The Read Model: Query Side**
[cite_start]**Reasoning:** The read side is optimized for high-performance retrieval and complex aggregations[cite: 328, 704].

**Query: Execute in ClickHouse**
```sql
CREATE TABLE order_summary (
    id UUID,
    amount Float64,
    status String,
    updated_at DateTime DEFAULT now()
) ENGINE = ReplacingMergeTree(updated_at)
ORDER BY id;
```

---

### **Exercise Validation**
1.  **Place a Command:** Create an order via your Spring Boot API (Write to Postgres).
2.  **Observe CDC:** Watch the Kafka logs; [cite_start]Debezium will emit a change event as soon as the transaction commits[cite: 421].
3.  **Verify Eventual Consistency:** Query ClickHouse. [cite_start]You will see the data appear within milliseconds, denormalized and ready for fast dashboards[cite: 381, 397].

### **Summary of Concerns Separated**
* [cite_start]**Write Concerns**: Validations, Transactional Safety, Relational Integrity[cite: 328, 373].
* [cite_start]**Read Concerns**: Fast aggregations, Columnar storage, UI-specific formatting[cite: 328, 378].