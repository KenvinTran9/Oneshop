package com.oneshop.service;

import com.oneshop.entity.Product;
import com.oneshop.entity.ProductStatus;
import com.oneshop.exception.ProductNotFoundException;
import com.oneshop.repository.ProductRepository;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public Page<Product> findActiveProducts(Pageable pageable) {
        return productRepository.findByStatus(ProductStatus.ACTIVE, pageable);
    }

    public List<Product> findFeaturedProducts() {
        return productRepository.findTop8ByStatusOrderByCreatedAtDesc(ProductStatus.ACTIVE);
    }

    public Product findActiveProductBySlug(String slug) {
        return productRepository.findBySlugAndStatus(slug, ProductStatus.ACTIVE)
                .orElseThrow(() -> new ProductNotFoundException(slug));
    }

    @Transactional
    public Product save(Product product) {
        return productRepository.save(product);
    }
}
