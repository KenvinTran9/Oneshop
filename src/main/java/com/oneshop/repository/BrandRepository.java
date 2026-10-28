package com.oneshop.repository;

import com.oneshop.entity.Brand;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BrandRepository extends MongoRepository<Brand, Long> {

    Optional<Brand> findBySlug(String slug);
}
