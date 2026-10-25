package com.oneshop.repository;

import com.oneshop.entity.Product;
import com.oneshop.entity.ProductStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    Optional<Product> findBySku(String sku);

    @EntityGraph(attributePaths = {"category", "brand"})
    @Query("""
            select p from Product p left join p.category c left join p.brand b
            where p.status = :status
              and (:category = '' or c.slug = :category)
              and (:search = '' or lower(p.name) like lower(concat('%', :search, '%'))
                   or lower(b.name) like lower(concat('%', :search, '%')))
            """)
    Page<Product> searchCatalog(@Param("status") ProductStatus status,
                                @Param("category") String category,
                                @Param("search") String search, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "brand"})
    Page<Product> findByStatusAndCategory_Slug(ProductStatus status, String categorySlug, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "brand"})
    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "brand"})
    List<Product> findTop8ByStatusOrderByCreatedAtDesc(ProductStatus status);

    @EntityGraph(attributePaths = {"category", "brand"})
    Optional<Product> findBySlugAndStatus(String slug, ProductStatus status);
}
