package com.example.orderprocessing.order.outbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "outbox_events")
public class OutboxEvent {

    @Id
    private UUID id;
    private UUID aggregateId;
    private String topic;
    private String messageKey;
    private String eventType;
    private String eventVersion;
    private String correlationId;
    @Lob
    @Column(nullable = false)
    private String payload;
    @Enumerated(EnumType.STRING)
    private OutboxStatus status;
    private int attempts;
    private String lastError;
    private Instant createdAt;
    private Instant publishedAt;

    protected OutboxEvent() {
    }

    public OutboxEvent(UUID id, UUID aggregateId, String topic, String messageKey, String eventType, String eventVersion, String correlationId, String payload, Instant createdAt) {
        this.id = id;
        this.aggregateId = aggregateId;
        this.topic = topic;
        this.messageKey = messageKey;
        this.eventType = eventType;
        this.eventVersion = eventVersion;
        this.correlationId = correlationId;
        this.payload = payload;
        this.status = OutboxStatus.PENDING;
        this.attempts = 0;
        this.createdAt = createdAt;
    }

    public void markPublished() {
        status = OutboxStatus.PUBLISHED;
        publishedAt = Instant.now();
        lastError = null;
    }

    public void markFailed(String error) {
        status = OutboxStatus.FAILED;
        attempts++;
        lastError = error;
    }

    public void markPendingForRetry() {
        status = OutboxStatus.PENDING;
    }

    public UUID getId() {
        return id;
    }

    public String getTopic() {
        return topic;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public String getEventType() {
        return eventType;
    }

    public String getEventVersion() {
        return eventVersion;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getPayload() {
        return payload;
    }
}
