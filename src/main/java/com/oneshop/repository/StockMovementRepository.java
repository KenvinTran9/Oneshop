package com.oneshop.repository;

import com.oneshop.entity.StockMovement;
import java.util.Optional;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select m from StockMovement m where m.requestToken = :token")
    Optional<StockMovement> findByRequestToken(@org.springframework.data.repository.query.Param("token") String token);
    @EntityGraph(attributePaths = {"store", "product"})
    Page<StockMovement> findByStoreId(Long storeId, Pageable pageable);
}
