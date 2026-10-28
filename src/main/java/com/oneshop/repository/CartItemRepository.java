package com.oneshop.repository;

import com.oneshop.entity.CartItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {

    Optional<CartItem> findByCartIdAndProductId(Long cartId, Long productId);

    List<CartItem> findAllByCartIdOrderByIdAsc(Long cartId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from CartItem item where item.cart.id = :cartId and item.product.id = :productId")
    int deleteByCartIdAndProductId(@Param("cartId") Long cartId, @Param("productId") Long productId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from CartItem item where item.cart.id = :cartId")
    int deleteAllByCartId(@Param("cartId") Long cartId);

    @Query("select coalesce(sum(item.quantity), 0) from CartItem item where item.cart.user.email = :email")
    long sumQuantityByCartUserEmail(@Param("email") String email);
}
