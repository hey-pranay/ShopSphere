package com.shopsphere.ecommerce.service;

import com.shopsphere.ecommerce.dto.payment.PaymentProcessRequest;
import com.shopsphere.ecommerce.dto.payment.PaymentProviderResult;
import com.shopsphere.ecommerce.dto.payment.PaymentResponse;
import com.shopsphere.ecommerce.entity.*;
import com.shopsphere.ecommerce.exception.*;
import com.shopsphere.ecommerce.mapper.PaymentMapper;
import com.shopsphere.ecommerce.repository.*;
import jakarta.persistence.EntityManager;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final PaymentAttemptRepository paymentAttemptRepository;
    private final EntityManager entityManager;
    private final PaymentAttemptService paymentAttemptService;
    private final PaymentProvider paymentProvider;


    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentMapper paymentMapper,
            OrderRepository orderRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            PaymentAttemptRepository paymentAttemptRepository,
            EntityManager entityManager,
            PaymentAttemptService paymentAttemptService,
            PaymentProvider paymentProvider
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentMapper = paymentMapper;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.entityManager = entityManager;
        this.paymentAttemptService = paymentAttemptService;
        this.paymentProvider = paymentProvider;
    }

    private User getAuthenticatedUser() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName();


        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new InvalidCredentialsException(
                                "Authenticated user not found"
                        ));
    }

    private Order getAuthorizedOrder(Long orderId) {
        User user = getAuthenticatedUser();

        if (user.getRole() != Role.ADMIN) {

            return orderRepository.findByIdAndUserId(
                    orderId,
                    user.getId()
            ).orElseThrow(() -> new OrderNotFoundException(
                            "Order with id " + orderId + " not found"
                    )
            );
        } else {
            return orderRepository.findById(orderId)
                    .orElseThrow(() ->
                            new OrderNotFoundException(
                                    "Order with id " + orderId + " not found"
                            )
                    );
        }
    }


    public Payment createPayment(Order order) {

        Payment payment = new Payment();

        payment.setOrder(order);

        payment.setAmount(order.getTotalAmount());

        payment.setStatus(PaymentStatus.PENDING);

        Payment savedPayment = paymentRepository.save(payment);

        return savedPayment;
    }

    public PaymentResponse getPaymentByOrderId(Long orderId) {

        Order order = getAuthorizedOrder(orderId);

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() ->
                        new PaymentNotFoundException(
                                "Payment for order " + orderId + " not found"
                        )
                );


        return paymentMapper.toResponse(payment);
    }

    @Transactional
    public PaymentResponse refundPayment(Long orderId) {
        Order order = getAuthorizedOrder(orderId);

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() ->
                        new PaymentNotFoundException(
                                "Payment for order " + orderId + " not found"
                        )
                );

        if (order.getStatus() != OrderStatus.CONFIRMED) {
            throw new InvalidOrderStateException(
                    "Order cannot be refunded in status " + order.getStatus()
            );
        }

        if (payment.getStatus() != PaymentStatus.SUCCESS) {
            throw new InvalidPaymentStatusException(
                    "Payment cannot be refunded in status " + payment.getStatus()
            );
        }

        payment.setStatus(PaymentStatus.REFUNDED);

        order.setStatus(OrderStatus.CANCELLED);

        for (OrderItem item : order.getItems()) {
            productRepository.increaseStock(
                    item.getProduct().getId(),
                    item.getQuantity()
            );
        }

        Payment savedPayment = paymentRepository.save(payment);

        return paymentMapper.toResponse(savedPayment);
    }


    @Transactional
    public PaymentResponse processPayment(
            Long orderId,
            PaymentProcessRequest request
    ) {

        Order order = getAuthorizedOrder(orderId);

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() ->
                        new PaymentNotFoundException(
                                "Payment for order " + orderId + " not found"
                        )
                );

        // check whether this exact payment attempt already exists
        Optional<PaymentAttempt> existingAttempt =
                paymentAttemptRepository.findByPaymentIdAndIdempotencyKey(
                        payment.getId(),
                        request.getIdempotencyKey()
                );

        if (existingAttempt.isPresent()) {
            PaymentAttempt attempt = existingAttempt.get();

            if (attempt.getStatus() == PaymentStatus.PROCESSING) {
                throw new InvalidPaymentStatusException(
                        "Payment attempt is still being processed"
                );
            }

            if (attempt.getStatus() == PaymentStatus.UNKNOWN) {

                // Payment was already completed successfully
                if (payment.getStatus() == PaymentStatus.SUCCESS) {

                    int attemptUpdated =
                            paymentAttemptRepository.markUnknownAttemptSuccessful(
                                    attempt.getId(),
                                    payment.getTransactionId()
                            );

                    if (attemptUpdated == 0) {
                        throw new InvalidPaymentStatusException(
                                "Payment attempt was already resolved"
                        );
                    }

                    entityManager.refresh(payment);

                    return paymentMapper.toResponse(payment);
                }

                // Payment was already failed
                if (payment.getStatus() == PaymentStatus.FAILED) {

                    int attemptUpdated =
                            paymentAttemptRepository.markUnknownAttemptFailed(
                                    attempt.getId()
                            );

                    if (attemptUpdated == 0) {
                        throw new InvalidPaymentStatusException(
                                "Payment attempt was already resolved"
                        );
                    }

                    entityManager.refresh(payment);

                    return paymentMapper.toResponse(payment);
                }

                // Payment is still pending, so we must ask the provider
                PaymentProviderResult providerResult =
                        paymentProvider.verifyPayment(
                                attempt.getIdempotencyKey()
                        );

                PaymentStatus providerStatus = providerResult.getStatus();


                if (providerStatus == PaymentStatus.SUCCESS) {

                    String transactionId = providerResult.getTransactionId();

                    if (transactionId == null || transactionId.isBlank()) {
                        throw new InvalidPaymentStatusException(
                                "Payment provider returned SUCCESS without a transaction ID"
                        );
                    }

                    validatePaymentTransition(
                            payment.getStatus(),
                            PaymentStatus.SUCCESS
                    );

                    int paymentUpdated =
                            paymentRepository.markPaymentSuccess(
                                    orderId,
                                    transactionId
                            );

                    if (paymentUpdated == 0) {
                        throw new InvalidPaymentStatusException(
                                "Payment was already processed"
                        );
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

                    order.setStatus(OrderStatus.PLACED);

                    entityManager.refresh(payment);

                    return paymentMapper.toResponse(payment);
                }

                if (providerStatus == PaymentStatus.FAILED) {

                    validatePaymentTransition(
                            payment.getStatus(),
                            PaymentStatus.FAILED
                    );

                    int paymentUpdated =
                            paymentRepository.markPaymentFailed(
                                    orderId,
                                    providerResult.getFailureReason()
                            );

                    if (paymentUpdated == 0) {
                        throw new InvalidPaymentStatusException(
                                "Payment was already processed"
                        );
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

                    order.setStatus(OrderStatus.CANCELLED);

                    for (OrderItem item : order.getItems()) {
                        productRepository.increaseStock(
                                item.getProduct().getId(),
                                item.getQuantity()
                        );
                    }

                    entityManager.refresh(payment);

                    return paymentMapper.toResponse(payment);
                }

                throw new InvalidPaymentStatusException(
                        "Payment provider could not determine payment status"
                );
            }


            payment.setStatus(attempt.getStatus());
            payment.setTransactionId(attempt.getTransactionId());

            return paymentMapper.toResponse(payment);
        }

        if (order.getPaymentExpiresAt() != null
                && order.getPaymentExpiresAt().isBefore(LocalDateTime.now())) {

            throw new InvalidOrderStateException(
                    "Payment window has expired for order " + orderId
            );
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStateException(
                    "Payment cannot be processed for order with status "
                            + order.getStatus()
            );
        }


        if (payment.getAmount().compareTo(order.getTotalAmount()) != 0) {
            throw new InvalidPaymentStatusException(
                    "Payment amount does not match order total"
            );
        }


        PaymentAttempt attempt;

        try {

            attempt = paymentAttemptService.createPaymentAttempt(
                    payment,
                    request.getIdempotencyKey()
            );

        } catch (DataIntegrityViolationException ex) {

            attempt = paymentAttemptRepository
                    .findByPaymentIdAndIdempotencyKey(
                            payment.getId(),
                            request.getIdempotencyKey()
                    )
                    .orElseThrow(() ->
                            new InvalidPaymentStatusException(
                                    "Payment attempt could not be created"
                            )
                    );
        }

        PaymentProviderResult providerResult =
                paymentProvider.processPayment(
                        orderId,
                        payment.getAmount(),
                        request.getIdempotencyKey()
                );

        PaymentStatus providerStatus = providerResult.getStatus();

        if (providerStatus == null) {
            throw new InvalidPaymentStatusException(
                    "Payment provider returned no payment status"
            );
        }


        if (providerStatus == PaymentStatus.SUCCESS) {

            String transactionId = providerResult.getTransactionId();

            if (transactionId == null || transactionId.isBlank()) {
                throw new InvalidPaymentStatusException(
                        "Payment provider returned SUCCESS without a transaction ID"
                );
            }

            int updated = paymentRepository.markPaymentSuccess(
                    orderId,
                    transactionId
            );

            validatePaymentTransition(
                    payment.getStatus(),
                    PaymentStatus.SUCCESS
            );

            if (updated == 0) {
                throw new InvalidPaymentStatusException(
                        "Payment has already been processed"
                );
            }

            paymentAttemptRepository.updateAttemptResult(
                    attempt.getId(),
                    PaymentStatus.SUCCESS,
                    transactionId
            );

            order.setStatus(OrderStatus.PLACED);

        } else if (providerStatus == PaymentStatus.FAILED) {

            String failureReason = providerResult.getFailureReason();

            if (failureReason == null || failureReason.isBlank()) {
                failureReason = "Payment failed without a reason from provider";
            }

            int updated = paymentRepository.markPaymentFailed(
                    orderId,
                    failureReason
            );

            validatePaymentTransition(
                    payment.getStatus(),
                    PaymentStatus.FAILED
            );

            if (updated == 0) {
                throw new InvalidPaymentStatusException(
                        "Payment has already been processed"
                );
            }

            paymentAttemptRepository.updateAttemptResult(
                    attempt.getId(),
                    PaymentStatus.FAILED,
                    null
            );

            order.setStatus(OrderStatus.CANCELLED);

            for (OrderItem item : order.getItems()) {

                productRepository.increaseStock(
                        item.getProduct().getId(),
                        item.getQuantity()
                );
            }
        } else {

            paymentAttemptRepository.markAttemptUnknown(
                    attempt.getId()
            );

            throw new InvalidPaymentStatusException(
                    "Payment provider could not determine payment status"
            );
        }


        entityManager.refresh(payment);

        return paymentMapper.toResponse(payment);
    }

    private void validatePaymentTransition(
            PaymentStatus current,
            PaymentStatus next
    ) {

        boolean allowed = switch (current) {

            case PENDING -> next == PaymentStatus.SUCCESS
                    || next == PaymentStatus.FAILED
                    || next == PaymentStatus.EXPIRED
                    || next == PaymentStatus.CANCELLED;

            case UNKNOWN -> next == PaymentStatus.SUCCESS
                    || next == PaymentStatus.FAILED;

            case SUCCESS -> next == PaymentStatus.REFUNDED;

            case PROCESSING -> next == PaymentStatus.SUCCESS
                    || next == PaymentStatus.FAILED
                    || next == PaymentStatus.UNKNOWN;

            case FAILED, EXPIRED, CANCELLED, REFUNDED -> false;
        };

        if (!allowed) {
            throw new InvalidPaymentStatusException(
                    "Invalid payment transition: "
                            + current
                            + " -> "
                            + next
            );
        }
    }

}