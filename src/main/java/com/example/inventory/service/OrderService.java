package com.example.inventory.service;

import com.example.inventory.exception.NotFoundException;
import com.example.inventory.model.Customer;
import com.example.inventory.model.CustomerOrder;
import com.example.inventory.model.OrderStatus;
import com.example.inventory.repository.CustomerOrderRepository;
import com.example.inventory.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.example.inventory.dto.AddOrderItemRequest;
import com.example.inventory.dto.UpdateOrderItemRequest;
import com.example.inventory.exception.BusinessRuleException;
import com.example.inventory.model.OrderItem;
import com.example.inventory.model.Product;
import com.example.inventory.repository.OrderItemRepository;
import com.example.inventory.repository.ProductRepository;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final CustomerOrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;

    public CustomerOrder createDraftOrder(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException("Customer not found with id: " + customerId));

        CustomerOrder order = new CustomerOrder();
        order.setCustomer(customer);
        order.setStatus(OrderStatus.DRAFT);
        order.setCreatedAt(LocalDateTime.now());

        return orderRepository.save(order);
    }

    public OrderItem addItemToOrder(Long orderId, AddOrderItemRequest request) {
        CustomerOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found with id: " + orderId));

        if (order.getStatus() != OrderStatus.DRAFT) {
            throw new BusinessRuleException("Cannot add items to order with status: " + order.getStatus());
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + request.getProductId()));

        // Check if product already exists in this order
        OrderItem existingItem = orderItemRepository.findByOrderIdAndProductId(orderId, request.getProductId()).orElse(null);

        if (existingItem != null) {
            // Update quantity if product already in order
            existingItem.setQuantity(existingItem.getQuantity() + request.getQuantity());
            return orderItemRepository.save(existingItem);
        }

        // Create new item
        OrderItem item = new OrderItem();
        item.setOrder(order);
        item.setProduct(product);
        item.setQuantity(request.getQuantity());

        return orderItemRepository.save(item);
    }

    public OrderItem updateOrderItemQuantity(Long orderId, Long itemId, UpdateOrderItemRequest request) {
        CustomerOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found with id: " + orderId));

        if (order.getStatus() != OrderStatus.DRAFT) {
            throw new BusinessRuleException("Cannot update items in order with status: " + order.getStatus());
        }

        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Order item not found with id: " + itemId));

        if (!item.getOrder().getId().equals(orderId)) {
            throw new NotFoundException("Order item not found in this order");
        }

        item.setQuantity(request.getQuantity());
        return orderItemRepository.save(item);
    }

    public void removeOrderItem(Long orderId, Long itemId) {
        CustomerOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found with id: " + orderId));

        if (order.getStatus() != OrderStatus.DRAFT) {
            throw new BusinessRuleException("Cannot remove items from order with status: " + order.getStatus());
        }

        OrderItem item = orderItemRepository.findById(itemId)
                .orElseThrow(() -> new NotFoundException("Order item not found with id: " + itemId));

        if (!item.getOrder().getId().equals(orderId)) {
            throw new NotFoundException("Order item not found in this order");
        }

        orderItemRepository.delete(item);
    }
}