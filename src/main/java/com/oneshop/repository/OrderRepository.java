package com.oneshop.repository;

import com.oneshop.entity.Order;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OrderRepository extends MongoRepository<Order, Long> {

    Optional<Order> findByCode(String code);
}
