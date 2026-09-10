package com.shopsphere.ecommerce.service;

import com.shopsphere.ecommerce.dto.payment.PaymentProviderResult;
import com.shopsphere.ecommerce.entity.PaymentStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MockPaymentProvider implements PaymentProvider {

    private PaymentStatus nextStatus = PaymentStatus.SUCCESS;

    private final Map<String, String> transactionIds = new ConcurrentHashMap<>();


    @Override
    public PaymentProviderResult processPayment(
            Long orderId,
            BigDecimal amount,
            String idempotencyKey
    ) {
        if (nextStatus == PaymentStatus.SUCCESS) {
            String transactionId =
                    transactionIds.computeIfAbsent(
                            idempotencyKey,
                            key -> "mock_txn_" + UUID.randomUUID()
                    );

            return new PaymentProviderResult(
                    PaymentStatus.SUCCESS,
                    transactionId,
                    null
            );
        }


        if (nextStatus == PaymentStatus.FAILED) {
            return new PaymentProviderResult(
                    PaymentStatus.FAILED,
                    null,
                    "Mock payment failed"
            );
        }

        return new PaymentProviderResult(
                PaymentStatus.UNKNOWN,
                null,
                "Mock provider could not determine payment status"
        );
    }

    @Override
    public PaymentProviderResult verifyPayment(String idempotencyKey) {

        if (nextStatus == PaymentStatus.SUCCESS) {

            String transactionId = transactionIds.get(idempotencyKey);

            if (transactionId == null) {
                return new PaymentProviderResult(
                        PaymentStatus.UNKNOWN,
                        null,
                        "Transaction ID not found for idempotency key"
                );
            }

            return new PaymentProviderResult(
                    PaymentStatus.SUCCESS,
                    transactionId,
                    null
            );
        }

        if (nextStatus == PaymentStatus.FAILED) {

            return new PaymentProviderResult(
                    PaymentStatus.FAILED,
                    null,
                    "Mock payment failed"
            );
        }

        return new PaymentProviderResult(
                PaymentStatus.UNKNOWN,
                null,
                "Mock provider could not determine payment status"
        );
    }

    public void setNextStatus(PaymentStatus status) {
        this.nextStatus = status;
    }
}