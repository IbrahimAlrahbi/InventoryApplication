package com.example.inventory.service;

import com.example.inventory.dto.CreateProductRequest;
import com.example.inventory.exception.NotFoundException;
import com.example.inventory.model.Category;
import com.example.inventory.model.Product;
import com.example.inventory.repository.CategoryRepository;
import com.example.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.example.inventory.dto.StockAdjustmentRequest;
import com.example.inventory.exception.BusinessRuleException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public Product createProduct(CreateProductRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new NotFoundException("Category not found with id: " + request.getCategoryId()));

        Product product = new Product();
        product.setName(request.getName());
        product.setPrice(request.getPrice());
        product.setStockQuantity(request.getStockQuantity());
        product.setCategory(category);

        return productRepository.save(product);
    }

    public Product adjustStock(Long productId, StockAdjustmentRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + productId));

        int newStock = product.getStockQuantity() + request.getQuantity();

        if (newStock < 0) {
            throw new BusinessRuleException(
                    "Cannot adjust stock. Current stock: " + product.getStockQuantity()
                            + ", Adjustment: " + request.getQuantity()
                            + ", Result would be: " + newStock
            );
        }

        product.setStockQuantity(newStock);
        return productRepository.save(product);
    }

    public List<Product> getLowStockProducts(Integer threshold) {
        return productRepository.findByStockQuantityLessThan(threshold);
    }

}