package com.example.orderprocessing.order.service;

import com.example.orderprocessing.order.dto.CreateOrderRequest;
import com.example.orderprocessing.order.dto.OrderLineResponse;
import com.example.orderprocessing.order.dto.OrderResponse;
import com.example.orderprocessing.order.entity.OrderLine;
import com.example.orderprocessing.order.entity.OrderStatus;
import com.example.orderprocessing.order.entity.PurchaseOrder;
import com.example.orderprocessing.order.event.OrderCreatedEvent;
import com.example.orderprocessing.order.event.OrderCreatedItem;
import com.example.orderprocessing.order.event.OrderEventPublisher;
import com.example.orderprocessing.order.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher orderEventPublisher;

    public OrderService(OrderRepository orderRepository, OrderEventPublisher orderEventPublisher) {
        this.orderRepository = orderRepository;
        this.orderEventPublisher = orderEventPublisher;
    }

    @Transactional
    public OrderResponse create(CreateOrderRequest request) {
        BigDecimal total = request.items().stream()
                .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        PurchaseOrder order = new PurchaseOrder(UUID.randomUUID(), request.customerId(), OrderStatus.CREATED, total, Instant.now());
        request.items().forEach(item -> order.addItem(new OrderLine(UUID.randomUUID(), item.productId(), item.quantity(), item.unitPrice())));

        PurchaseOrder saved = orderRepository.save(order);
        orderEventPublisher.publishOrderCreated(toEvent(saved));
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> findAll() {
        return orderRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse findById(UUID id) {
        return orderRepository.findById(id)
                .map(this::toResponse)
                .orElseThrow(() -> new IllegalArgumentException("Order not found: " + id));
    }

    private OrderCreatedEvent toEvent(PurchaseOrder order) {
        return new OrderCreatedEvent(
                UUID.randomUUID(),
                order.getId(),
                order.getCustomerId(),
                order.getTotalAmount(),
                Instant.now(),
                order.getItems().stream()
                        .map(item -> new OrderCreatedItem(item.getProductId(), item.getQuantity(), item.getUnitPrice()))
                        .toList()
        );
    }

    private OrderResponse toResponse(PurchaseOrder order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getStatus(),
                order.getTotalAmount(),
                order.getCreatedAt(),
                order.getItems().stream()
                        .map(item -> new OrderLineResponse(item.getId(), item.getProductId(), item.getQuantity(), item.getUnitPrice()))
                        .toList()
        );
    }
}
