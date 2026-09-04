package com.example.orderprocessing.notification.messaging.kafka;

import com.example.orderprocessing.notification.event.OrderCreatedEvent;
import com.example.orderprocessing.notification.idempotency.ProcessedEventService;
import com.example.orderprocessing.notification.service.NotificationService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OrderCreatedKafkaListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(OrderCreatedKafkaListener.class);
    private static final String EVENT_TYPE = "OrderCreatedEvent";

    private final NotificationService notificationService;
    private final ProcessedEventService processedEventService;

    public OrderCreatedKafkaListener(NotificationService notificationService, ProcessedEventService processedEventService) {
        this.notificationService = notificationService;
        this.processedEventService = processedEventService;
    }

    @Transactional
    @KafkaListener(
            topics = "${app.kafka.topics.orders-created.name}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "orderCreatedKafkaListenerContainerFactory"
    )
    public void listen(OrderCreatedEvent event, ConsumerRecord<String, OrderCreatedEvent> record) {
        LOGGER.info("Received eventId={} orderId={} topic={} partition={} offset={}",
                event.eventId(), event.orderId(), record.topic(), record.partition(), record.offset());

        if (processedEventService.alreadyProcessed(event.eventId())) {
            LOGGER.info("Skipping already processed eventId={} orderId={}", event.eventId(), event.orderId());
            return;
        }

        notificationService.handleOrderCreated(event);
        processedEventService.markProcessed(event.eventId(), EVENT_TYPE);
    }
}
