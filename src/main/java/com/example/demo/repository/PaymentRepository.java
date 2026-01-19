package com.example.demo.repository;

import com.example.demo.models.Payment;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentRepository extends MongoRepository<Payment, String> {
    Payment findByOrderId(String orderId);
}
