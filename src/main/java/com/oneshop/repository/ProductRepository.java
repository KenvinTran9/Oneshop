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

    default Page<Product> searchCatalog(ProductStatus status, String category, String search, Pageable pageable) {
        String literal = search == null ? "" : search.trim().replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return searchCatalogPattern(status, category == null ? "" : category, literal, pageable);
    }

    @EntityGraph(attributePaths = {"category", "brand"})
    @Query("""
            select p from Product p left join p.category c left join p.brand b
            where (p.status = :status or (:status = com.oneshop.entity.ProductStatus.ACTIVE and p.status = com.oneshop.entity.ProductStatus.OUT_OF_STOCK)) and p.active = true
              and (:category = '' or c.slug = :category)
              and (:search = '' or lower(p.name) like lower(concat('%', :search, '%')) escape '!'
                   or lower(b.name) like lower(concat('%', :search, '%')) escape '!')
            """)
    Page<Product> searchCatalogPattern(@Param("status") ProductStatus status,
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

    @Query(value = """
            select ranked.id from (
                select p.id, p.category_id,
                    row_number() over (partition by p.category_id order by p.created_at desc, p.id desc) as category_rank
                from products p where (p.status = :status or (:status = 'ACTIVE' and p.status = 'OUT_OF_STOCK')) and p.active = true and p.category_id is not null
            ) ranked where ranked.category_rank <= 2
            order by ranked.category_id, ranked.category_rank
            """, nativeQuery = true)
    List<Long> findHomeCategoryCandidateIds(@Param("status") String status, Pageable pageable);

    @EntityGraph(attributePaths = {"category", "brand"})
    @Query("select p from Product p where p.id in :ids and (p.status = :status or (:status = com.oneshop.entity.ProductStatus.ACTIVE and p.status = com.oneshop.entity.ProductStatus.OUT_OF_STOCK)) and p.active = true")
    List<Product> findHomeCandidates(@Param("ids") java.util.Collection<Long> ids, @Param("status") ProductStatus status);

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


    @Query("""
            select c.id as id, c.name as name, c.slug as slug, count(p) as productCount
            from Product p join p.category c where (p.status = :status or (:status = com.oneshop.entity.ProductStatus.ACTIVE and p.status = com.oneshop.entity.ProductStatus.OUT_OF_STOCK)) and p.active = true
            group by c.id, c.name, c.slug order by c.id
            """)
    List<HomeCategory> summarizeHomeCategories(@Param("status") ProductStatus status);

    @Query("""
            select b.name as name, count(p) as productCount
            from Product p join p.brand b where (p.status = :status or (:status = com.oneshop.entity.ProductStatus.ACTIVE and p.status = com.oneshop.entity.ProductStatus.OUT_OF_STOCK)) and p.active = true
            group by b.id, b.name order by count(p) desc, b.name asc
            """)
    List<HomeBrand> summarizeHomeBrands(@Param("status") ProductStatus status, Pageable pageable);


    @EntityGraph(attributePaths = {"category", "brand"})
    @Query("select p from Product p where p.slug = :slug and p.active = true and (p.status = :status or (:status = com.oneshop.entity.ProductStatus.ACTIVE and p.status = com.oneshop.entity.ProductStatus.OUT_OF_STOCK))")
    Optional<Product> findBySlugAndStatus(String slug, ProductStatus status);
}
