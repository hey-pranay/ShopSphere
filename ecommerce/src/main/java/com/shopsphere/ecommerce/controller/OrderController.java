package com.shopsphere.ecommerce.controller;

import com.shopsphere.ecommerce.dto.order.CreateOrderRequest;
import com.shopsphere.ecommerce.dto.order.OrderResponse;
import com.shopsphere.ecommerce.dto.order.OrderStatusUpdateRequest;
import com.shopsphere.ecommerce.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(
            OrderService orderService
    ) {
        this.orderService = orderService;
    }

    @PostMapping
    public OrderResponse createOrder(
            @Valid @RequestBody CreateOrderRequest request
    ) {
        return orderService.createOrder(request);
    }

    @GetMapping("/my-orders")
    public List<OrderResponse> getMyOrders() {
        return orderService.getMyOrders();
    }

    @PatchMapping("/{orderId}/cancel")
    public OrderResponse cancelOrder(
            @PathVariable Long orderId
    ) {
        return orderService.cancelOrder(orderId);
    }

    @PatchMapping("/{orderId}/status")
    public OrderResponse updateOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody OrderStatusUpdateRequest request
    ) {
        return orderService.updateOrderStatus(
                orderId,
                request.getStatus()
        );
    }
}