package com.example.orderprocessing.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record OrderItemRequest(
        @NotBlank String productId,
        @Min(1) int quantity,
        @NotNull @Positive BigDecimal unitPrice
) {
}
