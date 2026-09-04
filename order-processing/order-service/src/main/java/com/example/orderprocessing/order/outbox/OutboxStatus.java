package com.example.orderprocessing.order.outbox;

public enum OutboxStatus {
    PENDING,
    PUBLISHED,
    FAILED
}
