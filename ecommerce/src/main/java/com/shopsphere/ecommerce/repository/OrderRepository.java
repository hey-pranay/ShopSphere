package com.shopsphere.ecommerce.repository;

import com.shopsphere.ecommerce.entity.Order;
import com.shopsphere.ecommerce.entity.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    // IDOR (Insecure Direct Object Reference)
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<Order> findByIdAndUserId(Long orderId, Long userId);

    @Query("""
                    SELECT o
                    FROM Order o 
                    where o.status = :status
                        AND o.paymentExpiresAt <= :now
            """)
    List<Order> findExpiredOrders(
            @Param("status") OrderStatus status,
            @Param("now") LocalDateTime now
    );

}