package com.example.demo.client;

import com.example.demo.dto.PaymentResponse;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MockPaymentClient {

    public PaymentResponse initiatePayment(String orderId, Double amount) {
        PaymentResponse response = new PaymentResponse();
        response.setRazorpayOrderId("order_" + UUID.randomUUID().toString().substring(0, 14));
        response.setOrderId(orderId);
        response.setAmount(amount);
        response.setStatus("PENDING");
        response.setMessage("Payment initiated. Awaiting confirmation.");
        return response;
    }

    public boolean processPayment(String paymentId) {
        return Math.random() > 0.1;
    }
}
