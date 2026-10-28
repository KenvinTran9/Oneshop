package com.oneshop.repository;

import com.oneshop.entity.Role;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RoleRepository extends MongoRepository<Role, Long> {

    Optional<Role> findByName(String name);
}
