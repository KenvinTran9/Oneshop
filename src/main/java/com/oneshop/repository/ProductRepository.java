package com.oneshop.repository;
import com.oneshop.entity.Product;
import com.oneshop.entity.ProductStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
public interface ProductRepository extends MongoRepository<Product, Long>, ProductQueries {
    Optional<Product> findBySku(String sku);
    boolean existsBySkuIgnoreCase(String sku);
    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);
    boolean existsBySlugIgnoreCase(String slug);
    boolean existsBySlugIgnoreCaseAndIdNot(String slug, Long id);
    Page<Product> findByStatus(ProductStatus status, Pageable pageable);
    @Query(value="{'status':?0}", sort="{'createdAt':-1,'_id':-1}")
    List<Product> findByStatusOrderByCreatedAtDescIdDesc(ProductStatus status, Pageable pageable);
    @Query(value="{'status':?0,'category':?1}", sort="{'createdAt':-1,'_id':-1}")
    List<Product> findByStatusAndCategory_IdOrderByCreatedAtDescIdDesc(ProductStatus status, Long categoryId, Pageable pageable);
    Optional<Product> findBySlugAndStatus(String slug, ProductStatus status);
    interface HomeCategory { Long getId(); String getName(); String getSlug(); long getProductCount(); }
    interface HomeBrand { String getName(); long getProductCount(); }
    interface HomeCounts { long getProductCount(); long getBrandCount(); }
}
