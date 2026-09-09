package com.shopsphere.ecommerce.service;

import com.shopsphere.ecommerce.dto.payment.PaymentProviderResult;
import com.shopsphere.ecommerce.entity.PaymentStatus;

public interface PaymentProvider {

    PaymentProviderResult verifyPayment(String idempotencyKey);

}