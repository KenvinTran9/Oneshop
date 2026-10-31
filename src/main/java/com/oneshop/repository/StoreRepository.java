package com.oneshop.repository;

import com.oneshop.entity.Store;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRepository extends JpaRepository<Store, Long> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select s from Store s where s.id = :id")
    Optional<Store> lockById(@org.springframework.data.repository.query.Param("id") Long id);

    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    List<Store> findByActiveTrueOrderByNameAsc();
    List<Store> findByActiveTrueOrderByIdAsc();

    Optional<Store> findByCode(String code);
}
