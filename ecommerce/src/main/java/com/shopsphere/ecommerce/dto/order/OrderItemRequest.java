package com.shopsphere.ecommerce.dto.order;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class OrderItemRequest {

    @NotNull(message = "Product Id id required")
    private Long productId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must at least 1")
    @Max(value = 100 , message = "Quantity cannot exceed 100")
    private Integer quantity;


    public OrderItemRequest() {}

    public Long getProductId() {
        return productId;
    }

    public Integer getQuantity() {
        return quantity;
    }
}