package com.example.orderprocessing.order.messaging.kafka;

import com.example.orderprocessing.order.event.EventHeaders;
import com.example.orderprocessing.order.outbox.OutboxEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class OrderKafkaEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public OrderKafkaEventPublisher(KafkaTemplate<String, String> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(OutboxEvent event) {
        ProducerRecord<String, String> record = new ProducerRecord<>(event.getTopic(), event.getMessageKey(), event.getPayload());
        record.headers().add(EventHeaders.EVENT_TYPE, event.getEventType().getBytes(StandardCharsets.UTF_8));
        record.headers().add(EventHeaders.EVENT_VERSION, event.getEventVersion().getBytes(StandardCharsets.UTF_8));
        record.headers().add(EventHeaders.CORRELATION_ID, event.getCorrelationId().getBytes(StandardCharsets.UTF_8));
        kafkaTemplate.send(record).join();
    }
}
