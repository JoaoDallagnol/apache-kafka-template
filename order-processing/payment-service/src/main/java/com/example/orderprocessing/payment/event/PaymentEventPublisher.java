package com.example.orderprocessing.payment.event;

public interface PaymentEventPublisher {

    void publishPaymentProcessed(PaymentProcessedEvent event);
}
