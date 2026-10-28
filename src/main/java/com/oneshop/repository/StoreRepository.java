package com.oneshop.repository;

import com.oneshop.entity.Store;
import java.util.List;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface StoreRepository extends MongoRepository<Store, Long> {

    List<Store> findByActiveTrueOrderByNameAsc();

    Optional<Store> findByCode(String code);
}
