package com.example.orderprocessing.payment.event;

import org.springframework.stereotype.Component;

@Component
public class NoOpPaymentEventPublisher implements PaymentEventPublisher {

    @Override
    public void publishPaymentProcessed(PaymentProcessedEvent event) {
        // TODO Kafka: publish PaymentProcessedEvent to payments.processed.
    }
}
