package com.example.orderprocessing.order.messaging.kafka;

import com.example.orderprocessing.order.config.kafka.KafkaTopicProperties;
import com.example.orderprocessing.order.event.OrderCreatedEvent;
import com.example.orderprocessing.order.event.OrderEventPublisher;
import com.example.orderprocessing.order.outbox.OutboxEvent;
import com.example.orderprocessing.order.outbox.OutboxEventRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class OutboxOrderEventPublisher implements OrderEventPublisher {

    private static final String ORDER_CREATED_EVENT_TYPE = "OrderCreatedEvent";
    private static final String ORDER_CREATED_EVENT_VERSION = "1";

    private final KafkaTopicProperties topicProperties;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    public OutboxOrderEventPublisher(KafkaTopicProperties topicProperties, OutboxEventRepository outboxEventRepository, ObjectMapper objectMapper) {
        this.topicProperties = topicProperties;
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void publishOrderCreated(OrderCreatedEvent event) {
        try {
            outboxEventRepository.save(new OutboxEvent(
                    event.eventId(),
                    event.orderId(),
                    topicProperties.ordersCreated().name(),
                    event.orderId().toString(),
                    ORDER_CREATED_EVENT_TYPE,
                    ORDER_CREATED_EVENT_VERSION,
                    UUID.randomUUID().toString(),
                    objectMapper.writeValueAsString(event),
                    Instant.now()
            ));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not serialize OrderCreatedEvent", exception);
        }
    }
}
