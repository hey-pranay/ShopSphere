package com.shopsphere.ecommerce.service;

import com.shopsphere.ecommerce.dto.payment.PaymentProviderResult;
import com.shopsphere.ecommerce.entity.PaymentStatus;

import java.math.BigDecimal;

public interface PaymentProvider {

    PaymentProviderResult processPayment(
            Long orderId,
            BigDecimal amount,
            String idempotencyKey
    );

    PaymentProviderResult verifyPayment(String idempotencyKey);


}