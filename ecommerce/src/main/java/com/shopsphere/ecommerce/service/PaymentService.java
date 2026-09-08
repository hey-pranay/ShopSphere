package com.shopsphere.ecommerce.service;

import com.shopsphere.ecommerce.dto.payment.PaymentProcessRequest;
import com.shopsphere.ecommerce.dto.payment.PaymentResponse;
import com.shopsphere.ecommerce.entity.*;
import com.shopsphere.ecommerce.exception.*;
import com.shopsphere.ecommerce.mapper.PaymentMapper;
import com.shopsphere.ecommerce.repository.OrderRepository;
import com.shopsphere.ecommerce.repository.PaymentRepository;
import com.shopsphere.ecommerce.repository.ProductRepository;
import com.shopsphere.ecommerce.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;


    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentMapper paymentMapper,
            OrderRepository orderRepository,
            ProductRepository productRepository,
            UserRepository userRepository
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentMapper = paymentMapper;
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
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
    public PaymentResponse processPayment(
            Long orderId,
            PaymentProcessRequest request
    ) {

        Order order = getAuthorizedOrder(orderId);

        if (order.getStatus() != OrderStatus.PENDING) {
            throw new InvalidOrderStateException(
                    "Payment cannot be processed for order with status "
                            + order.getStatus()
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

            order.setStatus(OrderStatus.CANCELLED);

            for (OrderItem item : order.getItems()) {

                productRepository.increaseStock(
                        item.getProduct().getId(),
                        item.getQuantity()
                );
            }
        }

        Payment savedPayment = paymentRepository
                .findByOrderId(orderId)
                .orElseThrow(() ->
                        new PaymentNotFoundException(
                                "Payment for order " + orderId + " not found"
                        )
                );

        return paymentMapper.toResponse(savedPayment);
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


}