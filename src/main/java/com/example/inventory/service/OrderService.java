package com.example.inventory.service;

import com.example.inventory.exception.NotFoundException;
import com.example.inventory.model.Customer;
import com.example.inventory.model.CustomerOrder;
import com.example.inventory.model.OrderStatus;
import com.example.inventory.repository.CustomerOrderRepository;
import com.example.inventory.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final CustomerOrderRepository orderRepository;
    private final CustomerRepository customerRepository;

    public CustomerOrder createDraftOrder(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found with id: " + customerId));

        CustomerOrder order = new CustomerOrder();
        order.setCustomer(customer);
        order.setStatus(OrderStatus.DRAFT);
        order.setCreatedAt(LocalDateTime.now());

        return orderRepository.save(order);
    }
}