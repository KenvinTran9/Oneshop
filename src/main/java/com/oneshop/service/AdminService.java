package com.oneshop.service;

import com.oneshop.dto.response.AdminDashboardStatsResponse;
import com.oneshop.entity.Product;
import com.oneshop.repository.OrderRepository;
import com.oneshop.repository.ProductRepository;
import com.oneshop.repository.StoreRepository;
import com.oneshop.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AdminService {

    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public AdminService(ProductRepository productRepository,
                        StoreRepository storeRepository,
                        OrderRepository orderRepository,
                        UserRepository userRepository) {
        this.productRepository = productRepository;
        this.storeRepository = storeRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    public AdminDashboardStatsResponse getDashboardStats() {
        return new AdminDashboardStatsResponse(
                productRepository.count(),
                storeRepository.count(),
                orderRepository.count(),
                userRepository.count()
        );
    }

    public Page<Product> findProducts(Pageable pageable) {
        return productRepository.findAll(pageable);
    }
}
