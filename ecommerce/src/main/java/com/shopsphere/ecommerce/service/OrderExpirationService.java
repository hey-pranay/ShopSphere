package com.shopsphere.ecommerce.service;

import com.shopsphere.ecommerce.entity.Order;
import com.shopsphere.ecommerce.entity.OrderStatus;
import com.shopsphere.ecommerce.entity.Payment;
import com.shopsphere.ecommerce.entity.PaymentStatus;
import com.shopsphere.ecommerce.repository.OrderRepository;
import com.shopsphere.ecommerce.repository.PaymentRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderExpirationService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderService orderService;

    public OrderExpirationService(
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            OrderService orderService
    ) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.orderService = orderService;
    }

    @Scheduled(fixedRate = 60000) // every 60sec
    @Transactional
    public void expirePendingOrders() {
        List<Order> expiredOrders =
                orderRepository.findExpiredOrders(
                        OrderStatus.PENDING,
                        LocalDateTime.now()
                );

        for (Order order : expiredOrders) {

            Payment payment = paymentRepository
                    .findByOrderId(order.getId())
                    .orElse(null);

            if (payment == null) {
                continue;
            }

            if (payment.getStatus() != PaymentStatus.PENDING) {
                continue;
            }

            payment.setStatus(PaymentStatus.EXPIRED);
            paymentRepository.save(payment);

            orderService.cancelExpiredOrder(order);
        }
    }

}