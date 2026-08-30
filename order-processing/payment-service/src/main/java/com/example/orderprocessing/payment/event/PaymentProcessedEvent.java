package com.example.orderprocessing.payment.event;

import com.example.orderprocessing.payment.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentProcessedEvent(
        UUID eventId,
        UUID paymentId,
        UUID orderId,
        BigDecimal amount,
        PaymentStatus status,
        Instant occurredAt
) {
}
