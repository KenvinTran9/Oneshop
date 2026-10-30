package com.oneshop.repository;

import com.oneshop.entity.Cart;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CartRepository extends JpaRepository<Cart, Long> {
    @org.springframework.data.jpa.repository.Query("select coalesce(sum(i.quantity), 0) from CartItem i "
            + "join i.cart c where lower(c.user.email) = lower(:email)")
    long countItemsByUserEmail(@org.springframework.data.repository.query.Param("email") String email);

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Cart> findByUserEmail(String email);

    @EntityGraph(attributePaths = {"items", "items.product"})
    Optional<Cart> findByUserEmailIgnoreCase(String email);
}
