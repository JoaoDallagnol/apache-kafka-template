package com.example.orderprocessing.order.event;

import java.math.BigDecimal;

public record OrderCreatedItem(
        String productId,
        int quantity,
        BigDecimal unitPrice
) {
}
