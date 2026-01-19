package com.example.demo.repository;

import com.example.demo.models.Order_Item;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends MongoRepository<Order_Item, String> {
    java.util.List<Order_Item> findByOrderId(String orderId);
}
