package com.shopsphere.ecommerce.service;

import com.shopsphere.ecommerce.entity.PaymentStatus;
import org.springframework.stereotype.Service;

@Service
public class MockPaymentProvider implements PaymentProvider {

    private PaymentStatus nextStatus = PaymentStatus.FAILED;


    @Override
    public PaymentStatus verifyPayment(String idempotencyKey) {
        return nextStatus;
    }

    public void setNextStatus(PaymentStatus status) {
        this.nextStatus = status;
    }
}