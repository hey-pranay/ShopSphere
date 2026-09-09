package com.shopsphere.ecommerce.service;

import com.shopsphere.ecommerce.dto.payment.PaymentProcessRequest;
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

import java.util.Optional;
import java.util.UUID;

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

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentMapper paymentMapper,
            OrderRepository orderRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            PaymentAttemptRepository paymentAttemptRepository,
            EntityManager entityManager,
            PaymentAttemptService paymentAttemptService
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentMapper = paymentMapper;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.paymentAttemptRepository = paymentAttemptRepository;
        this.entityManager = entityManager;
        this.paymentAttemptService = paymentAttemptService;
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
                throw new InvalidPaymentStatusException(
                        "Payment attempt requires payment provider verification"
                );
            }

            payment.setStatus(attempt.getStatus());
            payment.setTransactionId(attempt.getTransactionId());

            return paymentMapper.toResponse(payment);
        }

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStateException(
                    "Payment cannot be processed for order with status "
                            + order.getStatus()
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

        String transactionId = UUID.randomUUID().toString();

        if (request.getSuccess()) {

            int updated = paymentRepository.markPaymentSuccess(
                    orderId,
                    transactionId
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

        } else {

            int updated = paymentRepository.markPaymentFailed(
                    orderId
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
        }


        entityManager.refresh(payment);

        return paymentMapper.toResponse(payment);
    }

}