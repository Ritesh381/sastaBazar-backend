package com.example.demo.controllers;

import com.example.demo.dto.PaymentRequest;
import com.example.demo.dto.PaymentResponse;
import com.example.demo.dto.PaymentVerificationRequest;
import com.example.demo.services.PaymentService;
import com.razorpay.RazorpayException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @PostMapping("/create-order")
    public ResponseEntity<PaymentResponse> createOrder(@RequestBody PaymentRequest request) {
        try {
            PaymentResponse response = paymentService.createOrder(request);

            if ("FAILED".equals(response.getStatus())) {
                return ResponseEntity.badRequest().body(response);
            }

            return ResponseEntity.ok(response);
        } catch (RazorpayException e) {
            PaymentResponse errorResponse = new PaymentResponse();
            errorResponse.setStatus("FAILED");
            errorResponse.setMessage("Error creating Razorpay order: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<PaymentResponse> verifyPayment(@RequestBody PaymentVerificationRequest request) {
        PaymentResponse response = paymentService.verifyPayment(request);

        if ("PAID".equals(response.getStatus())) {
            return ResponseEntity.ok(response);
        } else if ("NOT_FOUND".equals(response.getStatus())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } else {
            return ResponseEntity.badRequest().body(response);
        }
    }

    @GetMapping("/status/{orderId}")
    public ResponseEntity<PaymentResponse> getPaymentStatus(@PathVariable String orderId) {
        PaymentResponse response = paymentService.getPaymentStatus(orderId);

        if ("NOT_FOUND".equals(response.getStatus())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/failure")
    public ResponseEntity<PaymentResponse> handlePaymentFailure(
            @RequestParam String razorpayOrderId,
            @RequestParam(required = false) String errorMessage) {

        PaymentResponse response = paymentService.handlePaymentFailure(razorpayOrderId, errorMessage);

        if ("NOT_FOUND".equals(response.getStatus())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }

        return ResponseEntity.ok(response);
    }
}
