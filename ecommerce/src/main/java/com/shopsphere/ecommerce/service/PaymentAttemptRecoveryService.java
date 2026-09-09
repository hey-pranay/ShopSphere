package com.shopsphere.ecommerce.service;

import com.shopsphere.ecommerce.entity.PaymentAttempt;
import com.shopsphere.ecommerce.repository.PaymentAttemptRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaymentAttemptRecoveryService {

    private final PaymentAttemptRepository paymentAttemptRepository;

    public PaymentAttemptRecoveryService(
            PaymentAttemptRepository paymentAttemptRepository
    ) {
        this.paymentAttemptRepository = paymentAttemptRepository;
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void recoverStuckAttempts() {
        List<PaymentAttempt> stuckAttempts =
                paymentAttemptRepository.findStuckProcessingAttempts(
                        LocalDateTime.now()
                );

        for (PaymentAttempt attempt : stuckAttempts) {

            paymentAttemptRepository.markAttemptUnknown(
                    attempt.getId()
            );
        }
    }


}