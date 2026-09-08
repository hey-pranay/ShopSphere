package com.shopsphere.ecommerce.service;

import com.shopsphere.ecommerce.dto.payment.PaymentResponse;
import com.shopsphere.ecommerce.entity.Order;
import com.shopsphere.ecommerce.entity.Payment;
import com.shopsphere.ecommerce.entity.PaymentStatus;
import com.shopsphere.ecommerce.exception.PaymentNotFoundException;
import com.shopsphere.ecommerce.mapper.PaymentMapper;
import com.shopsphere.ecommerce.repository.PaymentRepository;
import org.springframework.stereotype.Service;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentMapper paymentMapper;

    public PaymentService(
            PaymentRepository paymentRepository,
            PaymentMapper paymentMapper
    ) {
        this.paymentRepository = paymentRepository;
        this.paymentMapper = paymentMapper;
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

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() ->
                        new PaymentNotFoundException(
                                "Payment does not exist for order with id " + orderId
                        ));

        return paymentMapper.toResponse(payment);
    }


}