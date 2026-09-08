package com.shopsphere.ecommerce.dto.payment;


import jakarta.validation.constraints.NotNull;

public class PaymentProcessRequest {


    @NotNull(message = "Payment success status is required")
    private Boolean success ;

    public PaymentProcessRequest() {
    }

    public Boolean getSuccess() {
        return success;
    }

    public void setSuccess(Boolean success) {
        this.success = success;
    }

}