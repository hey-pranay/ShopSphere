package com.shopsphere.ecommerce.repository;

import com.shopsphere.ecommerce.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public interface ProductRepository
        extends JpaRepository<Product, Long>,
        JpaSpecificationExecutor<Product>
{

    Page<Product> findByNameContainingIgnoreCase(
            String keyword,
            Pageable pageable
    );

    Page<Product> findByPriceBetween(
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Pageable pageable
    );

    boolean existsByCategoryId(Long categoryId);

    @Modifying
    @Query("""
                    UPDATE Product p
                    SET p.stockQuantity = p.stockQuantity - :quantity
                    WHERE p.id = :productId
                        AND p.active = true
                        AND p.stockQuantity >= :quantity
            """)
    int decreaseStock(
            @Param("productId") Long productId,
            @Param("quantity") Integer quantity
    );
}