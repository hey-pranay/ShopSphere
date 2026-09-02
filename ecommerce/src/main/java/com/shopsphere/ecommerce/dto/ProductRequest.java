package com.shopsphere.ecommerce.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public class ProductRequest {

    @NotBlank(message = "Product name is required")
    @Size(max = 150, message = "Product name must not exceed 150 characters")
    private String name;

    @Size(message = "Description must not exceed 200 characters")
    private String description;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.01", message = "Price must be greater than 0")
    private BigDecimal price;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = " Stock quantity cannot be negative")
    private Integer stockQuantity;

    private Boolean active;

    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    private String imageUrl;

    public ProductRequest() {
    }


    public ProductRequest(
            String name,
            String description,
            BigDecimal price,
            Integer stockQuantity,
            Boolean active,
            String imageUrl
    ) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.active = active;
        this.imageUrl = imageUrl;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    public Boolean getActive() {
        return active;
    }

    public String getImageUrl() {
        return imageUrl;
    }
}