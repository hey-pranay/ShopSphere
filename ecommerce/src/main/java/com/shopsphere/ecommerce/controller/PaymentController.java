package com.shopsphere.ecommerce.controller;

import com.shopsphere.ecommerce.dto.payment.PaymentProcessRequest;
import com.shopsphere.ecommerce.dto.payment.PaymentResponse;
import com.shopsphere.ecommerce.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/orders")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(
            PaymentService paymentService
    ) {
        this.paymentService = paymentService;
    }

    @GetMapping("/{orderId}/payment")
    public PaymentResponse getPaymentByOrderId(
            @PathVariable Long orderId
    ) {
        return paymentService.getPaymentByOrderId(orderId);
    }

    @PostMapping("/{orderId}/payment")
    public PaymentResponse processPayment(
            @PathVariable Long orderId,
            @Valid @RequestBody PaymentProcessRequest request
    ) {
        return paymentService.processPayment(orderId, request);
    }

    @PatchMapping("/{orderId}/payment/refund")
    public PaymentResponse refundPayment(
            @PathVariable Long orderId
    ) {
        return paymentService.refundPayment(orderId);
    }
}