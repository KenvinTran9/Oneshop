package com.oneshop.repository;

import com.oneshop.entity.Cart;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, Long> {

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Cart> findByUserEmail(String email);

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Cart> findByUserEmailIgnoreCase(String email);
}
