package com.example.orderprocessing.order.repository;

import com.example.orderprocessing.order.entity.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface OrderRepository extends JpaRepository<PurchaseOrder, UUID> {
}
