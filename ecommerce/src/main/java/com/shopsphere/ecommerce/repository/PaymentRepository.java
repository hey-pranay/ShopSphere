package com.shopsphere.ecommerce.repository;

import com.shopsphere.ecommerce.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrderId(Long orderId);

    @Modifying
    @Query("""
                UPDATE Payment p
                SET p.status = com.shopsphere.ecommerce.entity.PaymentStatus.EXPIRED
                WHERE p.order.id = :orderId
                  AND p.status = com.shopsphere.ecommerce.entity.PaymentStatus.PENDING
            """)
    int expirePayment(@Param("orderId") Long orderId);

}