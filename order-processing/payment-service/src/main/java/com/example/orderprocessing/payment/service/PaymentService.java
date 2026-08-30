package com.example.orderprocessing.payment.service;

import com.example.orderprocessing.payment.dto.CreatePaymentRequest;
import com.example.orderprocessing.payment.dto.PaymentResponse;
import com.example.orderprocessing.payment.entity.Payment;
import com.example.orderprocessing.payment.entity.PaymentStatus;
import com.example.orderprocessing.payment.event.OrderCreatedEvent;
import com.example.orderprocessing.payment.event.PaymentEventPublisher;
import com.example.orderprocessing.payment.event.PaymentProcessedEvent;
import com.example.orderprocessing.payment.repository.PaymentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class PaymentService {

    private static final BigDecimal AUTO_REJECT_THRESHOLD = BigDecimal.valueOf(1000);

    private final PaymentRepository paymentRepository;
    private final PaymentEventPublisher paymentEventPublisher;

    public PaymentService(PaymentRepository paymentRepository, PaymentEventPublisher paymentEventPublisher) {
        this.paymentRepository = paymentRepository;
        this.paymentEventPublisher = paymentEventPublisher;
    }

    @Transactional
    public PaymentResponse create(CreatePaymentRequest request) {
        Payment payment = process(request.orderId(), request.amount());
        return toResponse(payment);
    }

    @Transactional
    public void handleOrderCreated(OrderCreatedEvent event) {
        Payment payment = process(event.orderId(), event.totalAmount());
        paymentEventPublisher.publishPaymentProcessed(toEvent(payment));
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> findAll() {
        return paymentRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public PaymentResponse findById(UUID id) {
        return paymentRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + id));
    }

    private Payment process(UUID orderId, BigDecimal amount) {
        PaymentStatus status = amount.compareTo(AUTO_REJECT_THRESHOLD) > 0 ? PaymentStatus.REJECTED : PaymentStatus.APPROVED;
        return paymentRepository.save(new Payment(UUID.randomUUID(), orderId, amount, status, Instant.now()));
    }

    private PaymentProcessedEvent toEvent(Payment payment) {
        return new PaymentProcessedEvent(
                UUID.randomUUID(),
                payment.getId(),
                payment.getOrderId(),
                payment.getAmount(),
                payment.getStatus(),
                Instant.now()
        );
    }

    private PaymentResponse toResponse(Payment payment) {
        return new PaymentResponse(payment.getId(), payment.getOrderId(), payment.getAmount(), payment.getStatus(), payment.getCreatedAt());
    }
}
