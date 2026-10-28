package com.oneshop.repository;

import com.oneshop.entity.Category;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CategoryRepository extends MongoRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);
}
