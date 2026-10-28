package com.oneshop.repository;
import com.oneshop.entity.Cart;
import java.util.Optional;
public interface CartQueries {
    Optional<Cart> findByUserEmail(String email);
    Optional<Cart> findByUserEmailIgnoreCase(String email);
}
