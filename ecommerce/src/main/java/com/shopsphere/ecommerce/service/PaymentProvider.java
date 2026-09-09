package com.shopsphere.ecommerce.service;

import com.shopsphere.ecommerce.entity.PaymentStatus;

public interface PaymentProvider {

    PaymentStatus verifyPayment(String idempotencyKey);

}