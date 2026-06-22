package com.example.inventory.service;

import com.example.inventory.dto.CreateCustomerRequest;
import com.example.inventory.model.Customer;
import com.example.inventory.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    public Customer createCustomer(CreateCustomerRequest request) {
        Customer customer = new Customer();
        customer.setName(request.getName());
        customer.setEmail(request.getEmail());
        return customerRepository.save(customer);
    }
}