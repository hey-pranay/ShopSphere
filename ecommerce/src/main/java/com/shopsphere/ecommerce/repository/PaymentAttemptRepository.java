package com.shopsphere.ecommerce.repository;

import com.shopsphere.ecommerce.entity.PaymentAttempt;
import com.shopsphere.ecommerce.entity.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, Long> {

    Optional<PaymentAttempt> findByPaymentIdAndIdempotencyKey(
            Long paymentId,
            String idempotencyKey
    );


    boolean existsByPaymentIdAndIdempotencyKey(
            Long paymentId,
            String idempotencyKey
    );

    @Modifying
    @Query("""
                UPDATE PaymentAttempt pa
                SET pa.status = :status,
                    pa.transactionId = :transactionId
                WHERE pa.id = :attemptId
            """)
    int updateAttemptResult(
            @Param("attemptId") Long attemptId,
            @Param("status") PaymentStatus status,
            @Param("transactionId") String transactionId
    );

    @Modifying
    @Query("""
                UPDATE PaymentAttempt pa
                SET pa.status = com.shopsphere.ecommerce.entity.PaymentStatus.UNKNOWN
                WHERE pa.id = :attemptId
                  AND pa.status = com.shopsphere.ecommerce.entity.PaymentStatus.PROCESSING
            """)
    int markAttemptUnknown(@Param("attemptId") Long attemptId);


    @Query("""
                SELECT pa
                FROM PaymentAttempt pa
                JOIN FETCH pa.payment p
                JOIN FETCH p.order o
                WHERE pa.status = com.shopsphere.ecommerce.entity.PaymentStatus.PROCESSING
                  AND o.paymentExpiresAt <= :now
            """)
    List<PaymentAttempt> findStuckProcessingAttempts(
            @Param("now") LocalDateTime now
    );

    @Modifying
    @Query("""
                UPDATE PaymentAttempt pa
                SET pa.status = com.shopsphere.ecommerce.entity.PaymentStatus.SUCCESS,
                    pa.transactionId = :transactionId
                WHERE pa.id = :attemptId
                  AND pa.status = com.shopsphere.ecommerce.entity.PaymentStatus.UNKNOWN
            """)
    int markUnknownAttemptSuccessful(
            @Param("attemptId") Long attemptId,
            @Param("transactionId") String transactionId
    );

    @Modifying
    @Query("""
                UPDATE PaymentAttempt pa
                SET pa.status = com.shopsphere.ecommerce.entity.PaymentStatus.FAILED
                WHERE pa.id = :attemptId
                  AND pa.status = com.shopsphere.ecommerce.entity.PaymentStatus.UNKNOWN
            """)
    int markUnknownAttemptFailed(
            @Param("attemptId") Long attemptId
    );


    @Query("""
                SELECT COUNT(pa) > 0
                FROM PaymentAttempt pa
                WHERE pa.payment.order.id = :orderId
                  AND pa.status IN (
                      com.shopsphere.ecommerce.entity.PaymentStatus.PROCESSING,
                      com.shopsphere.ecommerce.entity.PaymentStatus.UNKNOWN
                  )
            """)
    boolean existsUnresolvedAttemptForOrder(
            @Param("orderId") Long orderId
    );

    @Query("""
                SELECT pa
                FROM PaymentAttempt pa
                JOIN FETCH pa.payment p
                JOIN FETCH p.order o
                WHERE pa.status = com.shopsphere.ecommerce.entity.PaymentStatus.UNKNOWN
            """)
    List<PaymentAttempt> findUnknownAttempts();

}