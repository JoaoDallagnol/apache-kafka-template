package com.example.orderprocessing.payment.messaging.kafka;

import com.example.orderprocessing.payment.config.kafka.KafkaTopicProperties;
import com.example.orderprocessing.payment.event.EventHeaders;
import com.example.orderprocessing.payment.event.PaymentEventPublisher;
import com.example.orderprocessing.payment.event.PaymentProcessedEvent;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Component
public class PaymentKafkaEventPublisher implements PaymentEventPublisher {

    private static final String EVENT_TYPE = "PaymentProcessedEvent";
    private static final String EVENT_VERSION = "1";

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaTopicProperties topicProperties;

    public PaymentKafkaEventPublisher(KafkaTemplate<String, Object> kafkaTemplate, KafkaTopicProperties topicProperties) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicProperties = topicProperties;
    }

    @Override
    public void publishPaymentProcessed(PaymentProcessedEvent event) {
        ProducerRecord<String, Object> record = new ProducerRecord<>(
                topicProperties.paymentsProcessed().name(),
                event.orderId().toString(),
                event
        );
        record.headers().add(EventHeaders.EVENT_TYPE, EVENT_TYPE.getBytes(StandardCharsets.UTF_8));
        record.headers().add(EventHeaders.EVENT_VERSION, EVENT_VERSION.getBytes(StandardCharsets.UTF_8));
        record.headers().add(EventHeaders.CORRELATION_ID, UUID.randomUUID().toString().getBytes(StandardCharsets.UTF_8));
        kafkaTemplate.send(record).join();
    }
}
