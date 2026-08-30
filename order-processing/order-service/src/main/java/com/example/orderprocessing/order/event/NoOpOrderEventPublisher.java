package com.example.orderprocessing.order.event;

import org.springframework.stereotype.Component;

@Component
public class NoOpOrderEventPublisher implements OrderEventPublisher {

    @Override
    public void publishOrderCreated(OrderCreatedEvent event) {
        // TODO Kafka: publish OrderCreatedEvent to orders.created.
    }
}
