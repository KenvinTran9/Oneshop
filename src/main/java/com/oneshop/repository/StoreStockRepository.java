package com.oneshop.repository;

import com.oneshop.entity.StoreStock;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoreStockRepository extends JpaRepository<StoreStock, Long> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from StoreStock s where s.store.id = :storeId and s.product.id in :productIds order by s.product.id")
    List<StoreStock> lockForOrder(@Param("storeId") Long storeId, @Param("productIds") java.util.Collection<Long> productIds);

    java.util.Optional<StoreStock> findByStoreIdAndProductId(Long storeId, Long productId);

    List<StoreStock> findByStoreId(Long storeId);

    List<StoreStock> findByProductId(Long productId);

    @Query("select coalesce(sum(stock.quantity), 0) from StoreStock stock "
            + "where stock.product.id = :productId and stock.active = true and stock.store.active = true")
    long sumAvailableQuantityByProductId(@Param("productId") Long productId);
}
