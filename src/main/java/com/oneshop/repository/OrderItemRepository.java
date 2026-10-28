package com.oneshop.repository;

import com.oneshop.entity.OrderItem;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OrderItemRepository extends MongoRepository<OrderItem, Long> {

    @org.springframework.data.mongodb.repository.Query("{'order': ?0}")
    List<OrderItem> findByOrderId(Long orderId);
}
