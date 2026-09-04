package com.shopsphere.ecommerce.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class CategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(max =  100, message = "Category name must not be exceed 100 characters")
    private String name;

    public CategoryRequest() {}

    public CategoryRequest(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}