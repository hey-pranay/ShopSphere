package com.shopsphere.ecommerce.dto.payment;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class PaymentProcessRequest {


    @NotNull(message = "Payment success status is required")
    private Boolean success;

    @NotBlank(message = "Idempotency key is required")
    private String idempotencyKey;

    public PaymentProcessRequest() {
    }

    public Boolean getSuccess() {
        return success;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }
}