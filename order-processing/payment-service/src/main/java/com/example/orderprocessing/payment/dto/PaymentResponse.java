package com.example.orderprocessing.payment.dto;

import com.example.orderprocessing.payment.entity.PaymentStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        UUID orderId,
        BigDecimal amount,
        PaymentStatus status,
        Instant createdAt
) {
}
