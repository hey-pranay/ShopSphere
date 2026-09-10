package com.shopsphere.ecommerce.service;

import com.shopsphere.ecommerce.dto.payment.PaymentProviderResult;
import com.shopsphere.ecommerce.entity.PaymentStatus;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class MockPaymentProvider implements PaymentProvider {

    private PaymentStatus nextStatus = PaymentStatus.FAILED;


    @Override
    public PaymentProviderResult processPayment(
            Long orderId,
            BigDecimal amount,
            String idempotencyKey
    ) {
        if (nextStatus == PaymentStatus.SUCCESS) {
            return new PaymentProviderResult(
                    PaymentStatus.SUCCESS,
                    "mock_txn_" + idempotencyKey,
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

            return new PaymentProviderResult(
                    PaymentStatus.SUCCESS,
                    "mock_txn_" + idempotencyKey,
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