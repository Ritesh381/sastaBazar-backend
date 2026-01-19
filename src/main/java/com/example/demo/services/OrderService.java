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

    public Order createOrder(String userId) {
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

        for (Cart_Item cartItem : cartItems) {
            Order_Item orderItem = new Order_Item();
            orderItem.setOrderId(savedOrder.getId());
            orderItem.setProductId(cartItem.getProductId());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setPrice(cartItem.getProduct().getPrice());
            orderItemRepository.save(orderItem);
        }

        cartService.clearCart(userId);
        return savedOrder;
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
