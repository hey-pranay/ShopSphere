package com.shopsphere.ecommerce.service;

import com.shopsphere.ecommerce.dto.payment.PaymentProviderResult;
import com.shopsphere.ecommerce.entity.OrderStatus;
import com.shopsphere.ecommerce.entity.PaymentAttempt;
import com.shopsphere.ecommerce.entity.PaymentStatus;
import com.shopsphere.ecommerce.exception.InvalidPaymentStatusException;
import com.shopsphere.ecommerce.repository.OrderRepository;
import com.shopsphere.ecommerce.repository.PaymentAttemptRepository;
import com.shopsphere.ecommerce.repository.PaymentRepository;
import com.shopsphere.ecommerce.repository.ProductRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaymentAttemptRecoveryService {

    private final PaymentAttemptRepository paymentAttemptRepository;
    private final PaymentProvider paymentProvider;
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    public PaymentAttemptRecoveryService(
            PaymentAttemptRepository paymentAttemptRepository,
            PaymentProvider paymentProvider,
            PaymentRepository paymentRepository,
            OrderRepository orderRepository,
            ProductRepository productRepository

    ) {
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.paymentProvider = paymentProvider;
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void recoverStuckAttempts() {

        // 1. Convert timed-out PROCESSING attempts to UNKNOWN
        List<PaymentAttempt> stuckAttempts =
                paymentAttemptRepository.findStuckProcessingAttempts(
                        LocalDateTime.now()
                );

        for (PaymentAttempt attempt : stuckAttempts) {

            paymentAttemptRepository.markAttemptUnknown(
                    attempt.getId()
            );
        }

        // 2. Find UNKNOWN attempts that need provider verification
        List<PaymentAttempt> unknownAttempts =
                paymentAttemptRepository.findUnknownAttempts();

        for (PaymentAttempt attempt : unknownAttempts) {

            PaymentProviderResult providerResult =
                    paymentProvider.verifyPayment(
                            attempt.getIdempotencyKey()
                    );

            PaymentStatus providerStatus = providerResult.getStatus();


            if (providerStatus == PaymentStatus.SUCCESS) {

                Long orderId =
                        attempt.getPayment()
                                .getOrder()
                                .getId();

                String transactionId = providerResult.getTransactionId();

                int paymentUpdated =
                        paymentRepository.markPaymentSuccess(
                                orderId,
                                transactionId
                        );

                if (paymentUpdated == 0) {
                    continue;
                }

                int attemptUpdated =
                        paymentAttemptRepository.markUnknownAttemptSuccessful(
                                attempt.getId(),
                                transactionId
                        );

                if (attemptUpdated == 0) {
                    throw new InvalidPaymentStatusException(
                            "Payment attempt was already resolved"
                    );
                }

                var order = attempt.getPayment().getOrder();

                order.setStatus(OrderStatus.PLACED);
            }

            System.out.println(
                    "Recovery check: attempt="
                            + attempt.getId()
                            + ", providerStatus="
                            + providerStatus
            );

            if (providerStatus == PaymentStatus.FAILED) {

                Long orderId =
                        attempt.getPayment()
                                .getOrder()
                                .getId();

                int paymentUpdated =
                        paymentRepository.markPaymentFailed(
                                orderId,
                                providerResult.getFailureReason()
                        );

                if (paymentUpdated == 0) {
                    continue;
                }

                int attemptUpdated =
                        paymentAttemptRepository.markUnknownAttemptFailed(
                                attempt.getId()
                        );

                if (attemptUpdated == 0) {
                    throw new InvalidPaymentStatusException(
                            "Payment attempt was already resolved"
                    );
                }

                var order = attempt.getPayment().getOrder();

                order.setStatus(OrderStatus.CANCELLED);

                for (var item : order.getItems()) {

                    productRepository.increaseStock(
                            item.getProduct().getId(),
                            item.getQuantity()
                    );
                }
            }
        }
    }


}