package com.shopsphere.ecommerce.specification;

import com.shopsphere.ecommerce.entity.Product;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

public class ProductSpecification {

    public static Specification<Product> nameContains(String keyword) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("name")),
                        "%" + keyword.toLowerCase() + "%"
                );
    }

    public static Specification<Product> priceGreaterThanOrEqual(
            BigDecimal minPrice
    ) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.greaterThanOrEqualTo(
                        root.get("price"),
                        minPrice
                );
    }

    public static Specification<Product> priceLessThanOrEqual(
            BigDecimal maxPrice
    ) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.lessThanOrEqualTo(
                        root.get("price"),
                        maxPrice
                );
    }

    public static Specification<Product> isActive(Boolean active) {
        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                        root.get("active"),
                        active
                );
    }

    public static Specification<Product> isInStock(Boolean inStock) {
        return (root, query, criteriaBuilder) -> {
            if (inStock) {
                return criteriaBuilder.greaterThan(
                        root.get("stockQuantity"),
                        0
                );
            }

            return criteriaBuilder.equal(
                    root.get("stockQuantity"),
                    0
            );
        };
    }

    public static Specification<Product> hasCategory(Long categoryId) {

        return (root, query, criteriaBuilder) ->
                criteriaBuilder.equal(
                        root.get("category").get("id"),
                        categoryId
                );
    }
}