package com.oneshop.repository;
import com.oneshop.entity.CustomerAddress;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CustomerAddressRepository extends JpaRepository<CustomerAddress,Long> {
    List<CustomerAddress> findByCustomerEmailOrderByIdDesc(String email);
    Optional<CustomerAddress> findByIdAndCustomerEmail(Long id,String email);
    long countByCustomerId(Long id);
}
