package com.example.orderprocessing.notification.dto;

import com.example.orderprocessing.notification.entity.NotificationType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateNotificationRequest(
        @NotNull UUID aggregateId,
        @NotNull NotificationType type,
        @NotBlank String recipient,
        @NotBlank String message
) {
}
