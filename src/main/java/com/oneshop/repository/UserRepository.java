package com.oneshop.repository;

import com.oneshop.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    @EntityGraph(attributePaths = {"role", "store"})
    @org.springframework.data.jpa.repository.Query("select u from User u where lower(u.email) like lower(concat('%',:q,'%')) or lower(u.fullName) like lower(concat('%',:q,'%')) order by u.id desc")
    org.springframework.data.domain.Page<User> search(@org.springframework.data.repository.query.Param("q") String q, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select u from User u where lower(u.email) = lower(:email)")
    Optional<User> findForCartChange(@org.springframework.data.repository.query.Param("email") String email);

    @EntityGraph(attributePaths = {"role"})
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);
}
