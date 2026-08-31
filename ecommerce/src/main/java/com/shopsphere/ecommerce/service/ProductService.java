package com.shopsphere.ecommerce.service;

import com.shopsphere.ecommerce.dto.ProductResponse;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class ProductService {
    public ProductResponse getProduct(int id) {
        return new ProductResponse(
                id,
                "Mechanical Keyboard",
                new BigDecimal("1599.00"),
                "Electronics"
        );
    }
}