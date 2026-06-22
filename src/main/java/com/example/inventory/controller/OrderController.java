package com.example.inventory.controller;

import com.example.inventory.model.CustomerOrder;
import com.example.inventory.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.inventory.dto.AddOrderItemRequest;
import com.example.inventory.dto.UpdateOrderItemRequest;
import com.example.inventory.model.OrderItem;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;


    @PostMapping("/customers/{customerId}/orders")
    public ResponseEntity<CustomerOrder> createDraftOrder(@PathVariable Long customerId) {
        CustomerOrder order = orderService.createDraftOrder(customerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @PostMapping("/orders/{orderId}/items")
    public ResponseEntity<OrderItem> addItemToOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody AddOrderItemRequest request) {
        OrderItem item = orderService.addItemToOrder(orderId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(item);
    }

    @PatchMapping("/orders/{orderId}/items/{itemId}")
    public ResponseEntity<OrderItem> updateOrderItemQuantity(
            @PathVariable Long orderId,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateOrderItemRequest request) {
        OrderItem item = orderService.updateOrderItemQuantity(orderId, itemId, request);
        return ResponseEntity.ok(item);
    }

    @DeleteMapping("/orders/{orderId}/items/{itemId}")
    public ResponseEntity<Void> removeOrderItem(
            @PathVariable Long orderId,
            @PathVariable Long itemId) {
        orderService.removeOrderItem(orderId, itemId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/orders/{orderId}/confirm")
    public ResponseEntity<CustomerOrder> confirmOrder(@PathVariable Long orderId) {
        CustomerOrder order = orderService.confirmOrder(orderId);
        return ResponseEntity.ok(order);
    }
}