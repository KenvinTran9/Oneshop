package com.oneshop.repository;

import com.oneshop.entity.Order;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {
    @org.springframework.data.jpa.repository.Query("select o from Order o where o.store.id = :storeId and (:status is null or o.status = :status)")
    org.springframework.data.domain.Page<Order> findBranchOrders(@org.springframework.data.repository.query.Param("storeId") Long storeId,
            @org.springframework.data.repository.query.Param("status") com.oneshop.entity.OrderStatus status, org.springframework.data.domain.Pageable pageable);

    Optional<Order> findByCode(String code);

    Optional<Order> findByCheckoutToken(String checkoutToken);
    Optional<Order> findByCodeAndCustomerEmailIgnoreCase(String code, String email);
    org.springframework.data.domain.Page<Order> findByCustomerEmailIgnoreCase(String email, org.springframework.data.domain.Pageable pageable);
    org.springframework.data.domain.Page<Order> findByStatus(com.oneshop.entity.OrderStatus status, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select o from Order o where o.code = :code")
    Optional<Order> findForUpdate(@org.springframework.data.repository.query.Param("code") String code);
}
