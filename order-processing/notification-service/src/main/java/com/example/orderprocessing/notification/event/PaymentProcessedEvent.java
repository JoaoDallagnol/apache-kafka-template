package com.example.orderprocessing.notification.event;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentProcessedEvent(
        UUID eventId,
        UUID paymentId,
        UUID orderId,
        BigDecimal amount,
        String status,
        Instant occurredAt
) {
}
