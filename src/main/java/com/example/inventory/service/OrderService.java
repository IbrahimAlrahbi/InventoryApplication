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
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;

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
    @Transactional
    public CustomerOrder confirmOrder(Long orderId) {
        CustomerOrder order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Order not found with id: " + orderId));

        if (order.getStatus() != OrderStatus.DRAFT) {
            throw new BusinessRuleException("Cannot confirm order with status: " + order.getStatus());
        }

        if (order.getItems().isEmpty()) {
            throw new BusinessRuleException("Cannot confirm an empty order");
        }

        // STEP 1: Check stock for ALL items first (all-or-nothing)
        for (OrderItem item : order.getItems()) {
            Product product = item.getProduct();

            if (product.getStockQuantity() < item.getQuantity()) {
                throw new BusinessRuleException("Insufficient stock for product: " + product.getName()
                        + ". Available: " + product.getStockQuantity()
                        + ", Required: " + item.getQuantity());
            }
        }

        // STEP 2: If all checks pass, lock products, reduce stock, and calculate totals
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItem item : order.getItems()) {
            // Lock the product row to prevent race conditions
            Product lockedProduct = productRepository.findByIdWithLock(item.getProduct().getId())
                    .orElseThrow(() -> new NotFoundException("Product not found with id: " + item.getProduct().getId()));

            // Re-verify stock with the locked row (safest approach)
            if (lockedProduct.getStockQuantity() < item.getQuantity()) {
                throw new BusinessRuleException("Insufficient stock for product: " + lockedProduct.getName()
                        + ". Available: " + lockedProduct.getStockQuantity()
                        + ", Required: " + item.getQuantity());
            }

            // Reduce stock
            lockedProduct.setStockQuantity(lockedProduct.getStockQuantity() - item.getQuantity());

            // Copy current price and calculate line total
            item.setUnitPriceAtConfirmation(lockedProduct.getPrice());
            BigDecimal lineTotal = lockedProduct.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
            item.setLineTotal(lineTotal);

            totalAmount = totalAmount.add(lineTotal);
        }
        // STEP 3: Update order details
        order.setTotalAmount(totalAmount);
        order.setStatus(OrderStatus.CONFIRMED);
        order.setConfirmedAt(LocalDateTime.now());

        return orderRepository.save(order);
    }
}