package com.example.demo.dto;

import com.example.demo.models.Order_Item;
import com.example.demo.models.Payment;
import com.example.demo.models.Status;

import java.time.Instant;
import java.util.List;

public class OrderResponse {
    private String id;
    private String userId;
    private Double totalAmount;
    private Status status;
    private Payment payment;
    private List<Order_Item> items;
    private Instant createdAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Double getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(Double totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public List<Order_Item> getItems() {
        return items;
    }

    public void setItems(List<Order_Item> items) {
        this.items = items;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
