package com.example.demo.services;

import com.example.demo.dto.OrderResponse;
import com.example.demo.models.Cart_Item;
import com.example.demo.models.Order;
import com.example.demo.models.Order_Item;
import com.example.demo.models.Status;
import com.example.demo.repository.OrderItemRepository;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;
    @Autowired
    private CartService cartService;
    @Autowired
    private PaymentRepository paymentRepository;

    public OrderResponse createOrder(String userId) {
        List<Cart_Item> cartItems = cartService.getCartItems(userId);
        if (cartItems.isEmpty()) {
            throw new RuntimeException("Cart is empty");
        }

        double totalAmount = cartItems.stream()
                .mapToDouble(item -> item.getProduct().getPrice() * item.getQuantity())
                .sum();

        Order order = new Order();
        order.setUserId(userId);
        order.setTotalAmount(totalAmount);
        order.setStatus(Status.CREATED);
        order.setCreatedAt(Instant.now());
        Order savedOrder = orderRepository.save(order);

        List<Order_Item> orderItems = new ArrayList<>();
        for (Cart_Item cartItem : cartItems) {
            Order_Item orderItem = new Order_Item();
            orderItem.setOrderId(savedOrder.getId());
            orderItem.setProductId(cartItem.getProductId());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(cartItem.getProduct().getPrice());
            Order_Item savedOrderItem = orderItemRepository.save(orderItem);
            orderItems.add(savedOrderItem);
        }

        cartService.clearCart(userId);

        // Build OrderResponse with items
        OrderResponse response = new OrderResponse();
        response.setId(savedOrder.getId());
        response.setUserId(savedOrder.getUserId());
        response.setTotalAmount(savedOrder.getTotalAmount());
        response.setStatus(savedOrder.getStatus());
        response.setCreatedAt(savedOrder.getCreatedAt());
        response.setItems(orderItems);

        return response;
    }

    public OrderResponse getOrder(String id) {
        Order order = orderRepository.findById(id).orElse(null);
        if (order == null) {
            return null;
        }

        OrderResponse response = new OrderResponse();
        response.setId(order.getId());
        response.setUserId(order.getUserId());
        response.setTotalAmount(order.getTotalAmount());
        response.setStatus(order.getStatus());
        response.setCreatedAt(order.getCreatedAt());

        response.setPayment(paymentRepository.findByOrderId(id));
        response.setItems(orderItemRepository.findByOrderId(id));

        return response;
    }
}
