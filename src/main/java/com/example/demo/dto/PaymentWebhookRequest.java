package com.example.demo.dto;

import java.util.Map;

/**
 * DTO for Razorpay Webhook events
 * This captures the webhook payload from Razorpay
 */
public class PaymentWebhookRequest {
    private String event;
    private String accountId;
    private Map<String, Object> payload;
    private Long createdAt;

    public PaymentWebhookRequest() {
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public Map<String, Object> getPayload() {
        return payload;
    }

    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }

    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }
}
