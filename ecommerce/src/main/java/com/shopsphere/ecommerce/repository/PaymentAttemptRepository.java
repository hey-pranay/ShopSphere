package com.shopsphere.ecommerce.repository;

import com.shopsphere.ecommerce.entity.PaymentAttempt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, Long> {

    Optional<PaymentAttempt> findByPaymentIdAndIdempotencyKey(
            Long paymentId,
            String idempotencyKey
    );

}