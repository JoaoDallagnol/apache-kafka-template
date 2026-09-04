# Order Processing

Order Processing is a small event-driven system built with three independent Spring Boot services and Kafka.

The project simulates a common microservice flow:

1. An order is created through `order-service`.
2. `order-service` publishes an `OrderCreatedEvent` to Kafka.
3. `payment-service` consumes the order event, processes the payment, and publishes a `PaymentProcessedEvent`.
4. `notification-service` consumes both order and payment events and stores notification records.

## Services

```text
order-processing/
  order-service/
  payment-service/
  notification-service/
  bruno/
  docs/
```

Each service is an independent Maven/Spring Boot project with its own `pom.xml`, Maven Wrapper, source code, and `application.yaml`.

## Kafka Architecture

Kafka is used as the asynchronous communication layer between services. Services do not call each other directly through HTTP for the main business flow. Instead, they publish facts that already happened and let interested services react to them.

Topics:

```text
orders.created
payments.processed
orders.created.dlt
```

Consumer groups:

```text
payment-service
notification-service
```

Message keys:

```text
orderId
```

Using `orderId` as the Kafka key keeps events for the same order in the same partition, preserving ordering for that aggregate.

## Topic Creation

Topics are created by Spring Kafka using `@Bean NewTopic`.

Main files:

```text
order-service/src/main/java/com/example/orderprocessing/order/config/kafka/KafkaTopicConfig.java
payment-service/src/main/java/com/example/orderprocessing/payment/config/kafka/KafkaTopicConfig.java
```

Topic names and topic settings live in `application.yaml`, under `app.kafka.topics`.

Example:

```yaml
app:
  kafka:
    topics:
      orders-created:
        name: orders.created
        partitions: 3
        replicas: 1
```

## Flow 1: Order Created

Request:

```http
POST http://localhost:8081/api/v1/orders
```

Execution:

1. `OrderController` receives the HTTP request.
2. `OrderService` creates and persists a `PurchaseOrder`.
3. `OrderService` builds an `OrderCreatedEvent`.
4. `OutboxOrderEventPublisher` stores the event in `outbox_events` in the same database transaction.
5. `OutboxEventPublisherScheduler` reads pending outbox events.
6. `OrderKafkaEventPublisher` publishes the event payload to `orders.created`.

Relevant files:

```text
order-service/src/main/java/com/example/orderprocessing/order/controller/OrderController.java
order-service/src/main/java/com/example/orderprocessing/order/service/OrderService.java
order-service/src/main/java/com/example/orderprocessing/order/messaging/kafka/OutboxOrderEventPublisher.java
order-service/src/main/java/com/example/orderprocessing/order/messaging/kafka/OrderKafkaEventPublisher.java
order-service/src/main/java/com/example/orderprocessing/order/outbox/OutboxEvent.java
order-service/src/main/java/com/example/orderprocessing/order/outbox/OutboxEventPublisherScheduler.java
```

## Flow 2: Payment Processed

Execution:

1. `payment-service` consumes `orders.created`.
2. `OrderCreatedKafkaListener` receives `OrderCreatedEvent`.
3. The listener checks `processed_events` to avoid processing the same event twice.
4. `PaymentService` creates one payment for the order.
5. `PaymentKafkaEventPublisher` publishes `PaymentProcessedEvent` to `payments.processed`.

Relevant files:

```text
payment-service/src/main/java/com/example/orderprocessing/payment/messaging/kafka/OrderCreatedKafkaListener.java
payment-service/src/main/java/com/example/orderprocessing/payment/service/PaymentService.java
payment-service/src/main/java/com/example/orderprocessing/payment/messaging/kafka/PaymentKafkaEventPublisher.java
payment-service/src/main/java/com/example/orderprocessing/payment/idempotency/ProcessedEvent.java
payment-service/src/main/java/com/example/orderprocessing/payment/idempotency/ProcessedEventService.java
```

## Flow 3: Notifications

Execution:

1. `notification-service` consumes `orders.created`.
2. It creates a notification that the order was received.
3. `notification-service` also consumes `payments.processed`.
4. It creates a notification with the payment result.
5. Both listeners use `processed_events` to avoid duplicate notifications.

Relevant files:

```text
notification-service/src/main/java/com/example/orderprocessing/notification/messaging/kafka/OrderCreatedKafkaListener.java
notification-service/src/main/java/com/example/orderprocessing/notification/messaging/kafka/PaymentProcessedKafkaListener.java
notification-service/src/main/java/com/example/orderprocessing/notification/service/NotificationService.java
notification-service/src/main/java/com/example/orderprocessing/notification/idempotency/ProcessedEvent.java
notification-service/src/main/java/com/example/orderprocessing/notification/idempotency/ProcessedEventService.java
```

## Reliability

### Outbox

`order-service` uses the outbox pattern to avoid the classic dual-write problem:

```text
save order in database
publish event to Kafka
```

Instead, it does:

```text
save order and outbox event in one transaction
publish pending outbox events asynchronously
```

This reduces the risk of saving an order without publishing the corresponding event.

### Producer Idempotence

Kafka producers are configured with:

```yaml
acks: all
enable.idempotence: true
retries: 3
```

This reduces duplicate messages caused by producer retries.

### Consumer Idempotence

Consumers persist processed event ids in:

```text
processed_events
```

If the same Kafka event is delivered again, the consumer skips it.

`payment-service` also protects the business rule with a unique `orderId`, so the same order cannot create multiple payments.

### Retry and DLT

`payment-service` retries failed `orders.created` messages with backoff. After retries are exhausted, the message is sent to:

```text
orders.created.dlt
```

This keeps the main consumer from retrying the same broken message forever.

## Event Headers

Produced Kafka messages include:

```text
eventType
eventVersion
correlationId
```

These headers help with debugging, tracing, and event evolution.

## Running Locally

Start Kafka first and expose it on:

```text
localhost:9092
```

Then start each service in a separate terminal:

```bash
cd order-service
./mvnw spring-boot:run
```

```bash
cd payment-service
./mvnw spring-boot:run
```

```bash
cd notification-service
./mvnw spring-boot:run
```

Ports:

```text
order-service: 8081
payment-service: 8082
notification-service: 8083
```

## Testing the Flow

Import the Bruno collection from:

```text
bruno/
```

Create an order with:

```text
Order / Create Order
```

Expected result:

1. A row is created in `orders`.
2. A row is created in `outbox_events`.
3. The outbox event is published to `orders.created`.
4. `payment-service` creates a payment.
5. `payment-service` publishes `payments.processed`.
6. `notification-service` creates notifications for the order and payment events.

## OpenAPI

API documentation files are available in:

```text
docs/order-service-openapi.yaml
docs/payment-service-openapi.yaml
docs/notification-service-openapi.yaml
```

## Build

Each service is built independently:

```bash
cd order-service
./mvnw -DskipTests compile
```

```bash
cd payment-service
./mvnw -DskipTests compile
```

```bash
cd notification-service
./mvnw -DskipTests compile
```
