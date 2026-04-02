This setup uses a single Spring Boot application to manage both the **Command** (PostgreSQL) and **Query** (ClickHouse) logic, separated by a **Debezium** CDC pipeline.

---

# **Hands-On Exercise: Transitioning to CQRS & CDC**

## **1. Infrastructure Setup**
**Reasoning:** To ensure structural separation, we use specialized containers for each concern.

### **Step 1.1: Docker Compose**
Create a `docker-compose.yml`:
```yaml
services:
  postgres:
    image: debezium/postgres:15
    environment:
      - POSTGRES_USER=user
      - POSTGRES_PASSWORD=pass
      - POSTGRES_DB=order_db
    ports: ["5432:5432"]

  kafka:
    image: bitnami/kafka:latest
    environment:
      - KAFKA_CFG_NODE_ID=1
      - KAFKA_CFG_PROCESS_ROLES=controller,broker
      - KAFKA_CFG_CONTROLLER_QUORUM_VOTERS=1@kafka:9093
      - KAFKA_CFG_LISTENERS=PLAINTEXT://:9092,CONTROLLER://:9093
      - KAFKA_CFG_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092
    ports: ["9092:9092"]

  debezium:
    image: debezium/connect:2.4
    ports: ["8083:8083"]
    environment:
      - BOOTSTRAP_SERVERS=kafka:9092
      - GROUP_ID=1
      - CONFIG_STORAGE_TOPIC=my_connect_configs
      - OFFSET_STORAGE_TOPIC=my_connect_offsets
      - STATUS_STORAGE_TOPIC=my_connect_statuses
    depends_on: [kafka, postgres]

  clickhouse:
    image: clickhouse/clickhouse-server:latest
    ports: ["8123:8123"]
```

---

## **2. Database & Kafka Initialization**
**Reasoning:** Manual initialization ensures that the CDC connector has a valid target and that the read model is ready to receive projections.

### **Step 2.1: Initialize PostgreSQL (Command DB)**
Run this command to enter the container and create the table:
```bash
docker exec -it $(docker ps -qf "name=postgres") psql -U user -d order_db -c "
CREATE TABLE orders (
    id UUID PRIMARY KEY,
    customer_id VARCHAR(255),
    amount DECIMAL,
    status VARCHAR(50)
);
ALTER TABLE orders REPLICA IDENTITY FULL;"
```

### **Step 2.2: Create Kafka Topic**
Debezium often creates topics automatically, but manual creation ensures proper partitioning:
```bash
docker exec -it $(docker ps -qf "name=kafka") /opt/bitnami/kafka/bin/kafka-topics.sh \
--create --topic order_events.public.orders --bootstrap-server localhost:9092
```

### **Step 2.3: Initialize ClickHouse (Read DB)**
Run this to create the denormalized read model:
```bash
docker exec -it $(docker ps -qf "name=clickhouse") clickhouse-client --query "
CREATE TABLE order_summary (
    id UUID,
    amount Float64,
    status String,
    updated_at DateTime DEFAULT now()
) ENGINE = ReplacingMergeTree(updated_at)
ORDER BY id;"
```

---

## **3. Spring Boot API Implementation**
**Reasoning:** A single service manages the "Command" entry point and the "Projection" listener.

### **3.1 Maven Dependencies (`pom.xml`)**
```xml
<dependencies>
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

### **3.2 The Controller (Write)**
```java
@PostMapping("/orders")
public ResponseEntity<OrderEntity> createOrder(@RequestBody OrderRequest req) {
    OrderEntity order = new OrderEntity(UUID.randomUUID(), req.customerId(), req.amount(), "PENDING");
    return ResponseEntity.ok(repository.save(order)); // Transactional write to Postgres
}
```

### **3.3 The Model Updater (Read)**
```java
@KafkaListener(topics = "order_events.public.orders")
public void project(String message) throws Exception {
    JsonNode payload = mapper.readTree(message).get("payload").get("after");
    clickHouseJdbc.update("INSERT INTO order_summary (id, amount, status) VALUES (?, ?, ?)", 
        payload.get("id").asText(), payload.get("amount").asDouble(), payload.get("status").asText());
}
```

---

## **4. Validation & Monitoring**

### **Step 4.1: Deploy Debezium Connector**
```bash
curl -i -X POST -H "Content-Type: application/json" localhost:8083/connectors/ -d '{
  "name": "order-connector",
  "config": {
    "connector.class": "io.debezium.connector.postgresql.PostgresConnector",
    "database.hostname": "postgres",
    "database.port": "5432",
    "database.user": "user",
    "database.password": "pass",
    "database.dbname": "order_db",
    "topic.prefix": "order_events",
    "table.include.list": "public.orders",
    "plugin.name": "pgoutput"
  }
}'
```

### **Step 4.2: Inspect CDC Events**
To see the raw JSON emitted by Debezium after a `POST /orders` call:
```bash
docker exec -it $(docker ps -qf "name=kafka") /opt/bitnami/kafka/bin/kafka-console-consumer.sh \
--bootstrap-server localhost:9092 --topic order_events.public.orders --from-beginning
```

---

### **How to generate the PDF**
1. Copy this Markdown content.
2. Visit a tool like **Dillinger.io** or **StackEdit.io**.
3. Paste the content and select **Export as PDF**.