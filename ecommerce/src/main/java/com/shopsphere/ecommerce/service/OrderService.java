package com.shopsphere.ecommerce.service;

import com.shopsphere.ecommerce.dto.order.CreateOrderRequest;
import com.shopsphere.ecommerce.dto.order.OrderItemRequest;
import com.shopsphere.ecommerce.dto.order.OrderResponse;
import com.shopsphere.ecommerce.entity.*;
import com.shopsphere.ecommerce.exception.*;
import com.shopsphere.ecommerce.mapper.OrderMapper;
import com.shopsphere.ecommerce.repository.OrderRepository;
import com.shopsphere.ecommerce.repository.PaymentRepository;
import com.shopsphere.ecommerce.repository.ProductRepository;
import com.shopsphere.ecommerce.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderMapper orderMapper;
    private final PaymentService paymentService;
    private final PaymentRepository paymentRepository;


    public OrderService(
            OrderRepository orderRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            OrderMapper orderMapper,
            PaymentService paymentService,
            PaymentRepository paymentRepository
    ) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.orderMapper = orderMapper;
        this.paymentService = paymentService;
        this.paymentRepository = paymentRepository;
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

    private void cancelOrderInternally(Order order) {
        if (order.getStatus() == OrderStatus.SHIPPED
                || order.getStatus() == OrderStatus.DELIVERED
                || order.getStatus() == OrderStatus.CANCELLED
        ) {
            throw new InvalidOrderStateException(
                    "Order cannot be cancelled in status " + order.getStatus()
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

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {

        User user = getAuthenticatedUser();

//        Detect duplicate products
        Set<Long> productIds = new HashSet<>();

        for (OrderItemRequest itemRequest : request.getItems()) {
            if (!productIds.add(itemRequest.getProductId())) {
                throw new DuplicateOrderItemException(
                        "Product with id " + itemRequest.getProductId()
                                + " appears more than once in the order"
                );
            }
        }

        Order order = new Order();

        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(BigDecimal.ZERO);
        order.setPaymentExpiresAt(
                LocalDateTime.now().plusMinutes(15)
        );
        BigDecimal totalAmount = BigDecimal.ZERO;

        for (OrderItemRequest itemRequest : request.getItems()) {

            Product product = productRepository.findById(
                    itemRequest.getProductId()
            ).orElseThrow(() ->
                    new ProductNotFoundException(
                            "Product with id " + itemRequest.getProductId() + " not found"
                    )
            );


            // optimistic locking race condition
            // to test the concurrency exception (2 users placing order at the same time when stock = 1)
//            try {
//                Thread.sleep(5000);
//            } catch (InterruptedException e) {
//                Thread.currentThread().interrupt();
//                throw new RuntimeException(e);
//            }

            if (!product.getActive()) {
                throw new ProductUnavailableException(
                        "Product with id " + product.getId() + " is not available"
                );
            }

//            if (product.getStockQuantity() < itemRequest.getQuantity()) {
//                throw new InsufficientStockException(
//                        "Insufficient stock for product with id " + product.getId()
//                );
//            }
//
//            product.setStockQuantity(
//                    product.getStockQuantity() - itemRequest.getQuantity()
//            );

            // atomic repo operation
            // atomic update
            int updated = productRepository.decreaseStock(
                    product.getId(),
                    itemRequest.getQuantity()
            );

            if (updated == 0) {
                throw new InsufficientStockException(
                        "Insufficient stock for product with id " + product.getId()
                );
            }

            OrderItem orderItem = new OrderItem();

            orderItem.setOrder(order);
            orderItem.setProduct(product);
            orderItem.setQuantity(itemRequest.getQuantity());

            BigDecimal unitPrice = product.getPrice();

            BigDecimal subtotal = unitPrice.multiply(
                    BigDecimal.valueOf(itemRequest.getQuantity())
            );

            totalAmount = totalAmount.add(subtotal);

            orderItem.setUnitPrice(unitPrice);
            orderItem.setSubtotal(subtotal);
            orderItem.setProductNameSnapshot(product.getName());

            order.getItems().add(orderItem);
        }

        order.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.save(order);

        paymentService.createPayment(savedOrder);
        return orderMapper.toResponse(savedOrder);
    }


    @Transactional(readOnly = true) // because we are only reading data
    public List<OrderResponse> getMyOrders() {
        User user = getAuthenticatedUser();

        List<Order> orders =
                orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        return orders.stream()
                .map(orderMapper::toResponse)
                .toList();
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId) {

        User user = getAuthenticatedUser();

        Order order = orderRepository
                .findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() ->
                        new OrderNotFoundException(
                                "Order with id " + orderId + " not found"
                        )
                );

        Payment payment = paymentRepository
                .findByOrderId(orderId)
                .orElseThrow(() ->
                        new PaymentNotFoundException(
                                "Payment for order " + orderId + " not found"
                        )
                );

        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new InvalidPaymentStatusException(
                    "Payment cannot be cancelled in status "
                            + payment.getStatus()
            );
        }

        cancelOrderInternally(order);

        payment.setStatus(PaymentStatus.CANCELLED);
        paymentRepository.save(payment);

        return orderMapper.toResponse(order);

    }

    private boolean isValidTransition(
            OrderStatus currentStatus,
            OrderStatus newStatus
    ) {
        return switch (currentStatus) {

            case PENDING, DELIVERED, CANCELLED -> false;

            case PLACED -> newStatus == OrderStatus.CONFIRMED;

            case CONFIRMED -> newStatus == OrderStatus.SHIPPED;

            case SHIPPED -> newStatus == OrderStatus.DELIVERED;

        };
    }

    @Transactional
    public OrderResponse updateOrderStatus(
            Long orderId,
            OrderStatus newStatus
    ) {


        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(
                                "Order with id " + orderId + " not found"
                        )
                );

        OrderStatus currentStatus = order.getStatus();

        if (!isValidTransition(currentStatus, newStatus)) {
            throw new InvalidOrderStateException(
                    "Cannot change order status from "
                            + currentStatus + " to " + newStatus
            );
        }

        order.setStatus(newStatus);
        orderRepository.save(order);

        return orderMapper.toResponse(order);
    }



}