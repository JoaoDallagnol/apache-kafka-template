package com.example.orderprocessing.order.event;

public interface OrderEventPublisher {

    void publishOrderCreated(OrderCreatedEvent event);
}
