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
    @Override
    @EntityGraph(attributePaths = {"category", "brand"})
    Optional<Product> findById(Long id);

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);

    boolean existsBySlugIgnoreCase(String slug);

    boolean existsBySlugIgnoreCaseAndIdNot(String slug, Long id);

    @Override
    @EntityGraph(attributePaths = {"category", "brand"})
    Page<Product> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"category", "brand"})
    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "brand"})
    List<Product> findByStatusOrderByCreatedAtDescIdDesc(ProductStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "brand"})
    List<Product> findByStatusAndCategory_IdOrderByCreatedAtDescIdDesc(
            ProductStatus status, Long categoryId, Pageable pageable);

    interface HomeCategory {
        Long getId();
        String getName();
        String getSlug();
        long getProductCount();
    }

    interface HomeBrand {
        String getName();
        long getProductCount();
    }

    interface HomeCounts {
        long getProductCount();
        long getBrandCount();
    }

    @Query("""
            select c.id as id, c.name as name, c.slug as slug, count(p) as productCount
            from Product p join p.category c where p.status = :status
            group by c.id, c.name, c.slug order by c.id
            """)
    List<HomeCategory> summarizeHomeCategories(@Param("status") ProductStatus status);

    @Query("""
            select b.name as name, count(p) as productCount
            from Product p join p.brand b where p.status = :status
            group by b.id, b.name order by count(p) desc, b.name asc
            """)
    List<HomeBrand> summarizeHomeBrands(@Param("status") ProductStatus status, Pageable pageable);

    @Query("""
            select count(p) as productCount, count(distinct b.id) as brandCount
            from Product p left join p.brand b where p.status = :status
            """)
    HomeCounts summarizeHomeCounts(@Param("status") ProductStatus status);

    @EntityGraph(attributePaths = {"category", "brand"})
    Optional<Product> findBySlugAndStatus(String slug, ProductStatus status);
}
