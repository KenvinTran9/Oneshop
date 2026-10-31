package com.oneshop.repository;

import com.oneshop.entity.ProductWatch;
import java.time.LocalDateTime;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

public interface ProductWatchRepository extends JpaRepository<ProductWatch,Long> {
    Optional<ProductWatch> findByUserIdAndProductId(Long userId,Long productId);
    long countByUserIdAndActiveTrue(Long userId);
    @EntityGraph(attributePaths={"product"})
    List<ProductWatch> findByUserEmailIgnoreCaseAndActiveTrueOrderByUpdatedAtDesc(String email);
    @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select w from ProductWatch w where w.id=:id") Optional<ProductWatch> lock(@Param("id") Long id);
    @Query("select w.id from ProductWatch w where w.active=true and (w.availableAt is null or (w.emailEnabled=true and w.notifiedAt is null)) and w.user.active=true "
        + "and w.product.active=true and w.product.status=com.oneshop.entity.ProductStatus.ACTIVE "
        + "and (w.lastAttemptAt is null or w.lastAttemptAt < :retryBefore) "
        + "and exists(select s.id from StoreStock s where s.product=w.product and s.active=true and s.store.active=true and s.quantity>0) order by w.id")
    List<Long> ready(@Param("retryBefore") LocalDateTime retryBefore,Pageable pageable);
    long countByUserEmailIgnoreCaseAndActiveTrueAndAvailableAtIsNotNullAndReadAtIsNull(String email);

}
