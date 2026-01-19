package com.example.demo.webhook;

import com.example.demo.dto.PaymentWebhookRequest;
import com.example.demo.models.Order;
import com.example.demo.models.Payment;
import com.example.demo.models.Status;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/webhooks")
public class PaymentWebhookController {

    @Value("${razorpay.key.secret}")
    private String webhookSecret;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;

    @PostMapping("/payment")
    public ResponseEntity<String> handleWebhook(
            @RequestBody PaymentWebhookRequest webhookRequest,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {

        try {
            String event = webhookRequest.getEvent();

            if (event == null) {
                return ResponseEntity.badRequest().body("Missing event type");
            }

            Map<String, Object> payload = webhookRequest.getPayload();

            switch (event) {
                case "payment.captured":
                    handlePaymentCaptured(payload);
                    break;

                case "payment.failed":
                    handlePaymentFailed(payload);
                    break;

                case "order.paid":
                    handleOrderPaid(payload);
                    break;

                default:
                    // Log unknown events but still return OK
                    System.out.println("Received webhook event: " + event);
            }

            return ResponseEntity.ok("Webhook processed successfully");

        } catch (Exception e) {
            System.err.println("Error processing webhook: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing webhook: " + e.getMessage());
        }
    }

    private void handlePaymentCaptured(Map<String, Object> payload) {
        try {
            Map<String, Object> paymentEntity = extractPaymentEntity(payload);
            if (paymentEntity == null)
                return;

            String orderId = extractRazorpayOrderId(paymentEntity);
            String paymentId = (String) paymentEntity.get("id");

            if (orderId != null) {
                Payment payment = paymentRepository.findByPaymentId(orderId);

                if (payment != null) {
                    payment.setStatus(Status.PAID);
                    payment.setPaymentId(paymentId);
                    paymentRepository.save(payment);

                    // Update order status
                    Optional<Order> orderOptional = orderRepository.findById(payment.getOrderId());
                    orderOptional.ifPresent(order -> {
                        order.setStatus(Status.PAID);
                        orderRepository.save(order);
                    });

                    System.out.println("Payment captured: " + paymentId);
                }
            }
        } catch (Exception e) {
            System.err.println("Error handling payment.captured: " + e.getMessage());
        }
    }

    private void handlePaymentFailed(Map<String, Object> payload) {
        try {
            Map<String, Object> paymentEntity = extractPaymentEntity(payload);
            if (paymentEntity == null)
                return;

            String orderId = extractRazorpayOrderId(paymentEntity);

            if (orderId != null) {
                Payment payment = paymentRepository.findByPaymentId(orderId);

                if (payment != null) {
                    payment.setStatus(Status.FAILED);
                    paymentRepository.save(payment);

                    // Update order status
                    Optional<Order> orderOptional = orderRepository.findById(payment.getOrderId());
                    orderOptional.ifPresent(order -> {
                        order.setStatus(Status.FAILED);
                        orderRepository.save(order);
                    });

                    System.out.println("Payment failed for order: " + orderId);
                }
            }
        } catch (Exception e) {
            System.err.println("Error handling payment.failed: " + e.getMessage());
        }
    }

    private void handleOrderPaid(Map<String, Object> payload) {
        try {
            Map<String, Object> orderEntity = extractOrderEntity(payload);
            if (orderEntity == null)
                return;

            String razorpayOrderId = (String) orderEntity.get("id");

            if (razorpayOrderId != null) {
                Payment payment = paymentRepository.findByPaymentId(razorpayOrderId);

                if (payment != null && payment.getStatus() != Status.PAID) {
                    payment.setStatus(Status.PAID);
                    paymentRepository.save(payment);

                    // Update order status
                    Optional<Order> orderOptional = orderRepository.findById(payment.getOrderId());
                    orderOptional.ifPresent(order -> {
                        order.setStatus(Status.PAID);
                        orderRepository.save(order);
                    });

                    System.out.println("Order paid: " + razorpayOrderId);
                }
            }
        } catch (Exception e) {
            System.err.println("Error handling order.paid: " + e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractPaymentEntity(Map<String, Object> payload) {
        if (payload == null)
            return null;
        Map<String, Object> payment = (Map<String, Object>) payload.get("payment");
        if (payment == null)
            return null;
        return (Map<String, Object>) payment.get("entity");
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractOrderEntity(Map<String, Object> payload) {
        if (payload == null)
            return null;
        Map<String, Object> order = (Map<String, Object>) payload.get("order");
        if (order == null)
            return null;
        return (Map<String, Object>) order.get("entity");
    }

    private String extractRazorpayOrderId(Map<String, Object> paymentEntity) {
        if (paymentEntity == null)
            return null;
        return (String) paymentEntity.get("order_id");
    }

    @SuppressWarnings("unused")
    private boolean verifyWebhookSignature(String payload, String signature, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes("UTF-8"), "HmacSHA256");
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

            return hexString.toString().equals(signature);
        } catch (Exception e) {
            return false;
        }
    }
}
