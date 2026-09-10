package com.shopsphere.ecommerce.dto.payment;


import jakarta.validation.constraints.NotBlank;

public class PaymentProcessRequest {


    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;

    public PaymentProcessRequest() {
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }
}