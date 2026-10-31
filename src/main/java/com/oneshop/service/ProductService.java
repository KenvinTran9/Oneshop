package com.oneshop.service;

import com.oneshop.entity.Product;
import com.oneshop.entity.ProductStatus;
import com.oneshop.exception.ProductNotFoundException;
import com.oneshop.repository.ProductRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    private final CatalogAvailability availability;
    public ProductService(ProductRepository productRepository, CatalogAvailability availability) {
        this.availability = availability;
        this.productRepository = productRepository;
    }

    public Page<Product> findActiveProducts(Pageable pageable) {
        return searchCatalog("", "", pageable);
    }

    public Page<Product> findActiveProducts(String categorySlug, Pageable pageable) {
        return categorySlug == null || categorySlug.isBlank()
                ? findActiveProducts(pageable)
                : searchCatalog(categorySlug, "", pageable);
    }

    public Page<Product> searchCatalog(String category, String search, Pageable pageable) {
        var page = productRepository.searchCatalog(ProductStatus.ACTIVE,
                category == null ? "" : category.trim(), search == null ? "" : search.trim(), pageable);
        availability.load(page.getContent());
        return page.map(this::withOptions);
    }

    public Product findActiveProductBySlug(String slug) {
        var product = productRepository.findBySlugAndStatus(slug, ProductStatus.ACTIVE)
                .orElseThrow(() -> new ProductNotFoundException(slug));
        availability.load(java.util.List.of(product));
        return withOptions(product);
    }

    private Product withOptions(Product product) {
        // Templates render after this read transaction closes (open-in-view=false).
        org.hibernate.Hibernate.initialize(product.getSizes());
        org.hibernate.Hibernate.initialize(product.getShades());
        return product;
    }

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public Product save(Product product) {
        return productRepository.save(product);
    }
}
