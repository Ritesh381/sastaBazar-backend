package com.example.demo.repository;

import com.example.demo.models.Cart_Item;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CartRepository extends MongoRepository<Cart_Item, String> {
    List<Cart_Item> findByUserId(String userId);
}
