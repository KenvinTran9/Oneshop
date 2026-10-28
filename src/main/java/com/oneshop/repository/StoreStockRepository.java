package com.oneshop.repository;
import com.oneshop.entity.StoreStock;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
public interface StoreStockRepository extends MongoRepository<StoreStock, Long>, StockQueries {
    @Query("{'store': ?0}") List<StoreStock> findByStoreId(Long storeId);
    @Query("{'product': ?0}") List<StoreStock> findByProductId(Long productId);
    @Query("{'store': ?0, 'product': ?1}") Optional<StoreStock> findByStoreIdAndProductId(Long storeId, Long productId);
}
