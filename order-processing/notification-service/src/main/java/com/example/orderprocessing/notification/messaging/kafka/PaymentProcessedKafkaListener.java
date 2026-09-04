package com.example.orderprocessing.notification.messaging.kafka;

import com.example.orderprocessing.notification.event.PaymentProcessedEvent;
import com.example.orderprocessing.notification.idempotency.ProcessedEventService;
import com.example.orderprocessing.notification.service.NotificationService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PaymentProcessedKafkaListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaymentProcessedKafkaListener.class);
    private static final String EVENT_TYPE = "PaymentProcessedEvent";

    private final NotificationService notificationService;
    private final ProcessedEventService processedEventService;

    public PaymentProcessedKafkaListener(NotificationService notificationService, ProcessedEventService processedEventService) {
        this.notificationService = notificationService;
        this.processedEventService = processedEventService;
    }

    @Transactional
    @KafkaListener(
            topics = "${app.kafka.topics.payments-processed.name}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "paymentProcessedKafkaListenerContainerFactory"
    )
    public void listen(PaymentProcessedEvent event, ConsumerRecord<String, PaymentProcessedEvent> record) {
        LOGGER.info("Received eventId={} orderId={} topic={} partition={} offset={}",
                event.eventId(), event.orderId(), record.topic(), record.partition(), record.offset());

        if (processedEventService.alreadyProcessed(event.eventId())) {
            LOGGER.info("Skipping already processed eventId={} orderId={}", event.eventId(), event.orderId());
            return;
        }

        notificationService.handlePaymentProcessed(event);
        processedEventService.markProcessed(event.eventId(), EVENT_TYPE);
    }
}
