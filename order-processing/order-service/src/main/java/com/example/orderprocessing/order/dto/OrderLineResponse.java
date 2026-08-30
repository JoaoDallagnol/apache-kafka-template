package com.example.orderprocessing.order.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record OrderLineResponse(
        UUID id,
        String productId,
        int quantity,
        BigDecimal unitPrice
) {
}
