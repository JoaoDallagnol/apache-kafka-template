package com.example.orderprocessing.notification.service;

import com.example.orderprocessing.notification.dto.CreateNotificationRequest;
import com.example.orderprocessing.notification.dto.NotificationResponse;
import com.example.orderprocessing.notification.entity.Notification;
import com.example.orderprocessing.notification.entity.NotificationType;
import com.example.orderprocessing.notification.event.OrderCreatedEvent;
import com.example.orderprocessing.notification.event.PaymentProcessedEvent;
import com.example.orderprocessing.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Transactional
    public NotificationResponse create(CreateNotificationRequest request) {
        Notification notification = new Notification(
                UUID.randomUUID(),
                request.aggregateId(),
                request.type(),
                request.recipient(),
                request.message(),
                Instant.now()
        );
        return toResponse(notificationRepository.save(notification));
    }

    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event) {
        save(event.orderId(), NotificationType.ORDER_CREATED, event.customerId(), "Order received: " + event.orderId());
    }

    @Transactional
    public void handlePaymentProcessed(PaymentProcessedEvent event) {
        NotificationType type = "APPROVED".equals(event.status()) ? NotificationType.PAYMENT_APPROVED : NotificationType.PAYMENT_REJECTED;
        save(event.orderId(), type, "customer", "Payment " + event.status().toLowerCase() + " for order " + event.orderId());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> findAll() {
        return notificationRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public NotificationResponse findById(UUID id) {
        return notificationRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found: " + id));
    }

    private void save(UUID aggregateId, NotificationType type, String recipient, String message) {
        notificationRepository.save(new Notification(UUID.randomUUID(), aggregateId, type, recipient, message, Instant.now()));
    }

    private NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getAggregateId(),
                notification.getType(),
                notification.getRecipient(),
                notification.getMessage(),
                notification.getCreatedAt()
        );
    }
}
