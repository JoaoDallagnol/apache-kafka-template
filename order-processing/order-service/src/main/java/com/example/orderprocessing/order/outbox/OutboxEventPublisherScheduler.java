package com.example.orderprocessing.order.outbox;

import com.example.orderprocessing.order.messaging.kafka.OrderKafkaEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class OutboxEventPublisherScheduler {

    private static final Logger LOGGER = LoggerFactory.getLogger(OutboxEventPublisherScheduler.class);

    private final OutboxEventRepository outboxEventRepository;
    private final OrderKafkaEventPublisher kafkaEventPublisher;

    public OutboxEventPublisherScheduler(OutboxEventRepository outboxEventRepository, OrderKafkaEventPublisher kafkaEventPublisher) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaEventPublisher = kafkaEventPublisher;
    }

    @Scheduled(fixedDelayString = "${app.kafka.outbox.fixed-delay:2000}")
    @Transactional
    public void publishPendingEvents() {
        outboxEventRepository.findTop20ByStatusOrderByCreatedAtAsc(OutboxStatus.PENDING)
                .forEach(this::publish);
    }

    private void publish(OutboxEvent event) {
        try {
            kafkaEventPublisher.publish(event);
            event.markPublished();
            LOGGER.info("Published outbox event eventId={} topic={} key={}", event.getId(), event.getTopic(), event.getMessageKey());
        } catch (RuntimeException exception) {
            event.markFailed(exception.getMessage());
            event.markPendingForRetry();
            LOGGER.warn("Failed to publish outbox event eventId={} topic={}", event.getId(), event.getTopic(), exception);
        }
    }
}
