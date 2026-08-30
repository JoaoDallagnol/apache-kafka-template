package com.example.orderprocessing.notification.event;

import java.math.BigDecimal;

public record OrderCreatedItem(
        String productId,
        int quantity,
        BigDecimal unitPrice
) {
}
