package com.shopsphere.ecommerce.service;

import com.shopsphere.ecommerce.dto.order.CreateOrderRequest;
import com.shopsphere.ecommerce.dto.order.OrderItemRequest;
import com.shopsphere.ecommerce.dto.order.OrderResponse;
import com.shopsphere.ecommerce.entity.*;
import com.shopsphere.ecommerce.exception.*;
import com.shopsphere.ecommerce.mapper.OrderMapper;
import com.shopsphere.ecommerce.repository.OrderRepository;
import com.shopsphere.ecommerce.repository.ProductRepository;
import com.shopsphere.ecommerce.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final OrderMapper orderMapper;


    public OrderService(
            OrderRepository orderRepository,
            ProductRepository productRepository,
            UserRepository userRepository,
            OrderMapper orderMapper
    ) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.orderMapper = orderMapper;
    }

    public User getAuthenticatedUser() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String email = authentication.getName();


        return userRepository.findByEmail(email)
                .orElseThrow(() ->
                        new InvalidCredentialsException(
                                "Authenticated user not found"
                        ));
    }



    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {

        User user = getAuthenticatedUser();

        Order order = new Order();

        order.setUser(user);
        order.setStatus(OrderStatus.PENDING);
        order.setTotalAmount(BigDecimal.ZERO);

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

            order.getItems().add(orderItem);
        }

        order.setTotalAmount(totalAmount);

        Order savedOrder = orderRepository.save(order);

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

        if (order.getStatus() == OrderStatus.SHIPPED
                || order.getStatus() == OrderStatus.DELIVERED
                || order.getStatus() == OrderStatus.CANCELLED
        ) {
            throw new InvalidOrderStateException(
                    "Order cannot be cancelled in status " + order.getStatus()
            );
        }

        for (OrderItem item : order.getItems()) {

            Product product = item.getProduct();

            product.setStockQuantity(
                    product.getStockQuantity()
                            + item.getQuantity()
            );
        }

        order.setStatus(OrderStatus.CANCELLED);

        return orderMapper.toResponse(order);

    }

}