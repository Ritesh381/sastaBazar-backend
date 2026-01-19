package com.example.demo.services;

import com.example.demo.dto.AddToCartRequest;
import com.example.demo.models.Cart_Item;
import com.example.demo.repository.CartRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CartService {
    @Autowired
    private CartRepository cartRepository;
    @Autowired
    private ProductService productService;

    public Cart_Item addToCart(AddToCartRequest addToCartRequest) {
        Cart_Item cartItem = new Cart_Item();
        cartItem.setQuantity(addToCartRequest.getQuantity());
        cartItem.setProductId(addToCartRequest.getProductId());
        cartItem.setUserId(addToCartRequest.getUserId());
        cartItem.setProduct(productService.getProductById(addToCartRequest.getProductId()));
        return cartRepository.save(cartItem);
    }

    public List<Cart_Item> getCartItems() {
        return cartRepository.findAll();
    }

    public List<Cart_Item> getCartItems(String userId) {
        return cartRepository.findByUserId(userId);
    }

    public String clearCart() {
        cartRepository.deleteAll();
        return "Cart cleared successfully";
    }

    public void clearCart(String userId) {
        List<Cart_Item> items = cartRepository.findByUserId(userId);
        cartRepository.deleteAll(items);
    }
}
