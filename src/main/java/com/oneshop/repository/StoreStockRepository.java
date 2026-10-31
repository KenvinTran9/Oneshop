package com.oneshop.repository;

import com.oneshop.entity.StoreStock;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StoreStockRepository extends JpaRepository<StoreStock, Long> {

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"store", "product"})
    @Query("select s from StoreStock s where s.store.id = :storeId and "
            + "(:low = false or s.quantity <= 5) and "
            + "(lower(s.product.name) like lower(concat('%',:query,'%')) or lower(s.product.sku) like lower(concat('%',:query,'%'))) order by s.product.name")
    org.springframework.data.domain.Page<StoreStock> search(@Param("storeId") Long storeId,
            @Param("query") String query, @Param("low") boolean low, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from StoreStock s where s.store.id = :storeId and s.product.id in :productIds order by s.product.id")
    List<StoreStock> lockForOrder(@Param("storeId") Long storeId, @Param("productIds") java.util.Collection<Long> productIds);

    java.util.Optional<StoreStock> findByStoreIdAndProductId(Long storeId, Long productId);

    List<StoreStock> findByStoreId(Long storeId);

    List<StoreStock> findByProductId(Long productId);

    @Query("select coalesce(sum(stock.quantity), 0) from StoreStock stock "
            + "where stock.product.id = :productId and stock.active = true and stock.store.active = true")
    long sumAvailableQuantityByProductId(@Param("productId") Long productId);
    interface Availability { Long getProductId(); long getQuantity(); }
    @Query("select s.product.id as productId, sum(s.quantity) as quantity from StoreStock s where s.product.id in :ids and s.active = true and s.store.active = true group by s.product.id")
    List<Availability> availability(@Param("ids") java.util.Collection<Long> ids);

}
