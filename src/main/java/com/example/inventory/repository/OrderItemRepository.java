package com.example.inventory.repository;

import com.example.inventory.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    // Needed later to check if product already exists in an order (Step 10)
    Optional<OrderItem> findByOrderIdAndProductId(Long orderId, Long productId);
}