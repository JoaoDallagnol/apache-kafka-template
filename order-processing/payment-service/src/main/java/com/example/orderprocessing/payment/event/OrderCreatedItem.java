package com.example.orderprocessing.payment.event;

import java.math.BigDecimal;

public record OrderCreatedItem(
        String productId,
        int quantity,
        BigDecimal unitPrice
) {
}
