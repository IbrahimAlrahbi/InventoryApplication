package com.example.inventory.controller;

import com.example.inventory.model.CustomerOrder;
import com.example.inventory.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
}