package com.example.orderprocessing.notification.dto;

import com.example.orderprocessing.notification.entity.NotificationType;

import java.time.Instant;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        UUID aggregateId,
        NotificationType type,
        String recipient,
        String message,
        Instant createdAt
) {
}
