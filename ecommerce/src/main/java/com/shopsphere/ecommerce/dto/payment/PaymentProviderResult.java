package com.shopsphere.ecommerce.dto.payment;

import com.shopsphere.ecommerce.entity.PaymentStatus;

public class PaymentProviderResult {
    private final PaymentStatus status;
    private final String transactionId;
    private final String failureReason;

    public PaymentProviderResult(
            PaymentStatus status,
            String transactionId,
            String failureReason
    ) {
        this.status = status;
        this.transactionId = transactionId;
        this.failureReason = failureReason;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public String getFailureReason() {
        return failureReason;
    }
}