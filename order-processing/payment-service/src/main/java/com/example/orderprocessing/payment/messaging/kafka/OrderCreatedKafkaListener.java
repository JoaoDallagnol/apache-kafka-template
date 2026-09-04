package com.example.orderprocessing.payment.messaging.kafka;

import com.example.orderprocessing.payment.event.OrderCreatedEvent;
import com.example.orderprocessing.payment.idempotency.ProcessedEventService;
import com.example.orderprocessing.payment.service.PaymentService;
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

    private final PaymentService paymentService;
    private final ProcessedEventService processedEventService;

    public OrderCreatedKafkaListener(PaymentService paymentService, ProcessedEventService processedEventService) {
        this.paymentService = paymentService;
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

        paymentService.handleOrderCreated(event);
        processedEventService.markProcessed(event.eventId(), EVENT_TYPE);
    }
}
