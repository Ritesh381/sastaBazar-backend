package com.example.demo.services;

import com.example.demo.config.RazorpayConfig;
import com.example.demo.dto.PaymentRequest;
import com.example.demo.dto.PaymentResponse;
import com.example.demo.dto.PaymentVerificationRequest;
import com.example.demo.models.Order;
import com.example.demo.models.Payment;
import com.example.demo.models.Status;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.PaymentRepository;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Optional;

@Service
public class PaymentService {

    @Autowired
    private RazorpayClient razorpayClient;

    @Autowired
    private RazorpayConfig razorpayConfig;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    public PaymentResponse createOrder(PaymentRequest request) throws RazorpayException {
        Optional<Order> orderOptional = orderRepository.findById(request.getOrderId());
        if (orderOptional.isEmpty()) {
            PaymentResponse errorResponse = new PaymentResponse();
            errorResponse.setStatus("FAILED");
            errorResponse.setMessage("Order not found with ID: " + request.getOrderId());
            return errorResponse;
        }

        Order order = orderOptional.get();

        // Amount in paise (multiply by 100 for INR)
        int amountInPaise = (int) (request.getAmount() * 100);
        String currency = request.getCurrency() != null ? request.getCurrency() : "INR";
        String receipt = request.getReceipt() != null ? request.getReceipt() : "order_" + request.getOrderId();

        // Create Razorpay order
        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", amountInPaise);
        orderRequest.put("currency", currency);
        orderRequest.put("receipt", receipt);

        JSONObject notes = new JSONObject();
        notes.put("order_id", request.getOrderId());
        if (request.getNotes() != null) {
            notes.put("notes_key_1", request.getNotes());
        }
        orderRequest.put("notes", notes);

        com.razorpay.Order razorpayOrder = razorpayClient.orders.create(orderRequest);

        // Save payment record to database
        Payment payment = new Payment();
        payment.setOrderId(request.getOrderId());
        payment.setAmount(request.getAmount());
        payment.setPaymentId(razorpayOrder.get("id")); // Razorpay order ID (order_xxx)
        payment.setStatus(Status.CREATED);
        payment.setCreatedAt(Instant.now());

        Payment savedPayment = paymentRepository.save(payment);

        // Update order status to CREATED (payment initiated)
        order.setStatus(Status.CREATED);
        orderRepository.save(order);

        // Build response
        PaymentResponse response = new PaymentResponse();
        response.setId(savedPayment.getId());
        response.setRazorpayOrderId(razorpayOrder.get("id"));
        response.setOrderId(request.getOrderId());
        response.setAmount(request.getAmount());
        response.setCurrency(currency);
        response.setStatus(razorpayOrder.get("status"));
        response.setReceipt(receipt);
        response.setMessage("Order created successfully");
        response.setRazorpayKeyId(razorpayConfig.getKeyId());

        return response;
    }

    public PaymentResponse verifyPayment(PaymentVerificationRequest request) {
        PaymentResponse response = new PaymentResponse();

        try {
            // Find payment by Razorpay order ID
            Payment payment = paymentRepository.findByPaymentId(request.getRazorpayOrderId());

            if (payment == null) {
                response.setStatus("FAILED");
                response.setMessage("Payment record not found");
                return response;
            }

            // Verify signature
            String generatedSignature = generateSignature(
                    request.getRazorpayOrderId(),
                    request.getRazorpayPaymentId(),
                    razorpayConfig.getKeySecret());

            if (generatedSignature.equals(request.getRazorpaySignature())) {
                // Payment verified successfully
                payment.setStatus(Status.PAID);
                payment.setRazorpayPaymentId(request.getRazorpayPaymentId()); // Store the actual payment ID
                paymentRepository.save(payment);

                // Update order status to PAID
                Optional<Order> orderOptional = orderRepository.findById(payment.getOrderId());
                if (orderOptional.isPresent()) {
                    Order order = orderOptional.get();
                    order.setStatus(Status.PAID);
                    orderRepository.save(order);
                }

                response.setId(payment.getId());
                response.setOrderId(payment.getOrderId());
                response.setRazorpayOrderId(request.getRazorpayOrderId());
                response.setAmount(payment.getAmount());
                response.setStatus("PAID");
                response.setMessage("Payment verified successfully");
            } else {
                // Signature mismatch - payment failed
                payment.setStatus(Status.FAILED);
                paymentRepository.save(payment);

                // Update order status to FAILED
                Optional<Order> orderOptional = orderRepository.findById(payment.getOrderId());
                if (orderOptional.isPresent()) {
                    Order order = orderOptional.get();
                    order.setStatus(Status.FAILED);
                    orderRepository.save(order);
                }

                response.setStatus("FAILED");
                response.setMessage("Payment signature verification failed");
            }
        } catch (Exception e) {
            response.setStatus("FAILED");
            response.setMessage("Error verifying payment: " + e.getMessage());
        }

        return response;
    }

    public PaymentResponse getPaymentStatus(String orderId) {
        PaymentResponse response = new PaymentResponse();

        Payment payment = paymentRepository.findByOrderId(orderId);

        if (payment == null) {
            response.setStatus("NOT_FOUND");
            response.setMessage("Payment not found for order ID: " + orderId);
            return response;
        }

        response.setId(payment.getId());
        response.setOrderId(payment.getOrderId());
        response.setRazorpayOrderId(payment.getPaymentId());
        response.setAmount(payment.getAmount());
        response.setStatus(payment.getStatus().toString());
        response.setMessage("Payment status retrieved successfully");

        return response;
    }

    private String generateSignature(String orderId, String paymentId, String secret) throws Exception {
        String payload = orderId + "|" + paymentId;

        javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA256");
        javax.crypto.spec.SecretKeySpec secretKeySpec = new javax.crypto.spec.SecretKeySpec(
                secret.getBytes("UTF-8"),
                "HmacSHA256");
        mac.init(secretKeySpec);

        byte[] hash = mac.doFinal(payload.getBytes("UTF-8"));

        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }

        return hexString.toString();
    }

    public PaymentResponse handlePaymentFailure(String razorpayOrderId, String errorMessage) {
        PaymentResponse response = new PaymentResponse();

        Payment payment = paymentRepository.findByPaymentId(razorpayOrderId);

        if (payment != null) {
            payment.setStatus(Status.FAILED);
            paymentRepository.save(payment);

            // Update order status to FAILED
            Optional<Order> orderOptional = orderRepository.findById(payment.getOrderId());
            if (orderOptional.isPresent()) {
                Order order = orderOptional.get();
                order.setStatus(Status.FAILED);
                orderRepository.save(order);
            }

            response.setId(payment.getId());
            response.setOrderId(payment.getOrderId());
            response.setRazorpayOrderId(razorpayOrderId);
            response.setStatus("FAILED");
            response.setMessage(errorMessage != null ? errorMessage : "Payment failed");
        } else {
            response.setStatus("NOT_FOUND");
            response.setMessage("Payment record not found");
        }

        return response;
    }
}
