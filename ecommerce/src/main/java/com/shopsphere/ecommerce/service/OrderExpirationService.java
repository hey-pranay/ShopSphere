package com.shopsphere.ecommerce.service;

import com.shopsphere.ecommerce.entity.Order;
import com.shopsphere.ecommerce.entity.OrderItem;
import com.shopsphere.ecommerce.entity.OrderStatus;
import com.shopsphere.ecommerce.exception.InvalidOrderStateException;
import com.shopsphere.ecommerce.exception.OrderNotFoundException;
import com.shopsphere.ecommerce.repository.OrderRepository;
import com.shopsphere.ecommerce.repository.PaymentAttemptRepository;
import com.shopsphere.ecommerce.repository.PaymentRepository;
import com.shopsphere.ecommerce.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrderExpirationService {

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final EntityManager entityManager;
    private final PaymentAttemptRepository paymentAttemptRepository;


    public OrderExpirationService(
            OrderRepository orderRepository,
            PaymentRepository paymentRepository,
            ProductRepository productRepository,
            EntityManager entityManager,
            PaymentAttemptRepository paymentAttemptRepository
    ) {
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.entityManager = entityManager;
        this.productRepository = productRepository;
        this.paymentAttemptRepository = paymentAttemptRepository;
    }

    @Scheduled(fixedRate = 60000)
    @Transactional
    public void expirePendingOrders() {

        List<Order> expiredOrders =
                orderRepository.findExpiredOrders(
                        OrderStatus.PENDING,
                        LocalDateTime.now()
                );

        for (Order order : expiredOrders) {

            if (paymentAttemptRepository.existsUnresolvedAttemptForOrder(
                    order.getId()
            )) {
                continue;
            }

            // Atomically claim the payment:
            // PENDING → EXPIRED
            int paymentUpdated = paymentRepository.expirePayment(
                    order.getId()
            );

            if (paymentUpdated == 0) {
                // Payment was already processed by another transaction
                continue;
            }

            // Atomically claim the order:
            // PENDING → CANCELLED
            int orderUpdated = orderRepository.cancelExpiredOrder(
                    order.getId(),
                    LocalDateTime.now()
            );

            if (orderUpdated == 0) {
                throw new InvalidOrderStateException(
                        "Order " + order.getId()
                                + " could not be expired because its status changed"
                );
            }

            entityManager.clear();

            Order cancelledOrder = orderRepository.findById(order.getId())
                    .orElseThrow(() ->
                            new OrderNotFoundException(
                                    "Order with id " + order.getId() + " not found"
                            )
                    );

            // Restore reserved stock
            for (OrderItem item : cancelledOrder.getItems()) {

                productRepository.increaseStock(
                        item.getProduct().getId(),
                        item.getQuantity()
                );
            }
        }
    }

}