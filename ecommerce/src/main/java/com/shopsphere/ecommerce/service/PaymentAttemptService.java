package com.shopsphere.ecommerce.service;

import com.shopsphere.ecommerce.entity.Payment;
import com.shopsphere.ecommerce.entity.PaymentAttempt;
import com.shopsphere.ecommerce.entity.PaymentStatus;
import com.shopsphere.ecommerce.repository.PaymentAttemptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentAttemptService {

    private final PaymentAttemptRepository paymentAttemptRepository;

    public PaymentAttemptService(
            PaymentAttemptRepository paymentAttemptRepository
    ) {
        this.paymentAttemptRepository = paymentAttemptRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PaymentAttempt createPaymentAttempt(
            Payment payment,
            String idempotencyKey
    ) {

        PaymentAttempt attempt = new PaymentAttempt();

        attempt.setPayment(payment);
        attempt.setIdempotencyKey(idempotencyKey);
        attempt.setAmount(payment.getAmount());
        attempt.setStatus(PaymentStatus.PROCESSING);

        return paymentAttemptRepository.saveAndFlush(attempt);
    }

}