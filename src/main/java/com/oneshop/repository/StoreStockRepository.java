package com.oneshop.repository;

import com.oneshop.entity.StoreStock;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreStockRepository extends JpaRepository<StoreStock, Long> {

    List<StoreStock> findByStoreId(Long storeId);

    List<StoreStock> findByProductId(Long productId);
}
