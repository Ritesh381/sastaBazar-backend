package com.example.demo.controllers;

import com.example.demo.dto.AddToCartRequest;
import com.example.demo.models.Cart_Item;
import com.example.demo.services.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    @Autowired
    private CartService cartService;

    @PostMapping("/add")
    public ResponseEntity<Cart_Item> addToCart(@RequestBody AddToCartRequest request) {
        Cart_Item cartItem = cartService.addToCart(request);
        return ResponseEntity.ok(cartItem);
    }

    @GetMapping
    public ResponseEntity<List<Cart_Item>> getCart() {
        List<Cart_Item> cartItems = cartService.getCartItems();
        return ResponseEntity.ok(cartItems);
    }

    @DeleteMapping("/clear")
    public ResponseEntity<String> clearCart() {
        String message = cartService.clearCart();
        return ResponseEntity.ok(message);
    }
}
